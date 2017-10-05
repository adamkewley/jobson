/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.github.jobson.jobs.states;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jobson.api.v1.JobId;
import com.github.jobson.api.v1.UserId;
import com.github.jobson.jobinputs.JobExpectedInputId;
import com.github.jobson.jobinputs.JobInput;
import com.github.jobson.specs.JobSpec;

import java.util.Map;

/**
 * Used internally after a resolved job is persisted (i.e. assigned a JobID and
 * queued).
 */
public class PersistedJobRequest extends ValidJobRequest {

    @JsonProperty
    private JobId id;



    /**
     * @deprecated Used by JSON deserializer
     */
    public PersistedJobRequest() {}

    public PersistedJobRequest(
            JobId id,
            UserId owner,
            String name,
            Map<JobExpectedInputId, JobInput> inputs,
            JobSpec spec) {

        super(owner, name, inputs, spec);
        this.id = id;
    }



    public JobId getId() {
        return this.id;
    }
}
