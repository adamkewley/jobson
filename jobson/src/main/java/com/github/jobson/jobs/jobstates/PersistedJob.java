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

package com.github.jobson.jobs.jobstates;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jobson.api.v1.UserId;
import com.github.jobson.jobinputs.JobExpectedInputId;
import com.github.jobson.jobinputs.JobInput;
import com.github.jobson.jobs.JobId;
import com.github.jobson.jobs.JobTimestamp;
import com.github.jobson.specs.JobSpec;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

import static com.github.jobson.jobs.JobStatus.SUBMITTED;
import static java.util.Collections.singletonList;

/**
 * Used internally after a resolved job is persisted (i.e. assigned a JobID and
 * queued).
 */
public class PersistedJob extends ValidJobRequest {

    public static PersistedJob createFromValidRequest(ValidJobRequest validJobRequest, JobId jobId) {
        return new PersistedJob(
                jobId,
                validJobRequest.getOwner(),
                validJobRequest.getName(),
                validJobRequest.getInputs(),
                singletonList(JobTimestamp.now(SUBMITTED, "Job persisted")),
                validJobRequest.getSpec());
    }


    @JsonProperty
    private JobId id;

    @Schema(description = "Timestamps indicating when job status changes occurred")
    @JsonProperty
    private List<JobTimestamp> timestamps;



    /**
     * @deprecated Used by JSON deserializer
     */
    public PersistedJob() {}

    public PersistedJob(
            JobId id,
            UserId owner,
            String name,
            Map<JobExpectedInputId, JobInput> inputs,
            List<JobTimestamp> timestamps,
            JobSpec spec) {

        super(owner, name, inputs, spec);
        this.id = id;
        this.timestamps = timestamps;
    }



    public JobId getId() {
        return this.id;
    }

    public List<JobTimestamp> getTimestamps() {
        return timestamps;
    }


    public PersistedJob withSpec(JobSpec spec) {
        return new PersistedJob(
                this.getId(),
                this.getOwner(),
                this.getName(),
                this.getInputs(),
                this.getTimestamps(),
                spec);
    }
}
