/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.hpe.caf.api.autoscale;


/**
 * A WorkloadAnalyser examines the workload of a service and makes
 * recommendations upon how to scale it at a given time.
 */
public interface WorkloadAnalyser
{
    /**
     * Analyse the workload of a service given information on its
     * current instances, and make a recommendation on how to scale it.
     * @param instanceInfo information on the currently running instances of a service
     * @return a recommendation on how to scale this service (if at all)
     * @throws ScalerException if the workload analysis fails
     */
    ScalingAction analyseWorkload(InstanceInfo instanceInfo)
        throws ScalerException;
}