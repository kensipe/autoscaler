/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.autoscale.scaler.marathon;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.hpe.caf.api.autoscale.ScalerException;
import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AppInstancePatcher {

    private final static Logger LOG = LoggerFactory.getLogger(AppInstancePatcher.class);
    private final URI marathonUri;

    public AppInstancePatcher(final URI marathonUri){
        this.marathonUri = marathonUri;
    }

    public void patchInstances(final String appId, final int instances) throws ScalerException {
        patchInstances(appId, instances, false);
    }

    private void patchInstances(final String appId, final int instances, final boolean force) throws ScalerException {
        final JsonObject details = new JsonObject();
        details.addProperty("id", appId);
        details.addProperty("instances", instances);

        final JsonArray appArray = new JsonArray();
        appArray.add(details);
        
        try(final CloseableHttpClient client = HttpClientBuilder.create().build()){
            final URIBuilder uriBuilder = new URIBuilder(marathonUri).setPath("/v2/apps");
            uriBuilder.setParameters(Arrays.asList(new BasicNameValuePair("force", Boolean.toString(force))));
            final HttpPatch patch = new HttpPatch(uriBuilder.build());
            patch.setEntity(new StringEntity(appArray.toString(), ContentType.APPLICATION_JSON));
            int waitTime = 1000;
            int waitTimeMultiplier = 0;
            while (true) {
                final HttpResponse response = client.execute(patch);
                if (response.getStatusLine().getStatusCode() == 200) {
                    return;
                } else if (!force && response.getStatusLine().getStatusCode() == 409) {
                    patchInstances(appId, instances, true);
                    return;
                } else if (response.getStatusLine().getStatusCode() == 503) {
                    LOG.error("An error occured during the attempt to patch the service {}", appId);
                    LOG.error("Recieved response code {}", response.getStatusLine().getStatusCode());
                    LOG.error("Recieved reason phrase {}", response.getStatusLine().getReasonPhrase());
                    LOG.error("Retrying service patch request after backoff period.");
                } else {
                    LOG.error("An error occured during the attempt to patch the service {}", appId);
                    LOG.error("Recieved response code {}", response.getStatusLine().getStatusCode());
                    LOG.error("Recieved reason phrase {}", response.getStatusLine().getReasonPhrase());
                    throw new ScalerException(response.getStatusLine().getReasonPhrase());
                }
                final long timeToWait = waitTime * (waitTimeMultiplier < 10 ? waitTimeMultiplier : 10);
                Thread.sleep(timeToWait);
                waitTimeMultiplier++;
                LOG.error("Retry count: " + waitTimeMultiplier);
            }
        } catch (URISyntaxException | IOException | InterruptedException ex) {
            throw new ScalerException(String.format("Exception patching %s to %s instances.", appId, instances), ex);
        }
    }
}
