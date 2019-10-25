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

package com.github.jobson.websockets.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jobson.jobs.JobId;
import com.github.jobson.jobs.JobStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response to a request for a job spec's details")
public final class JobEvent {

    @JsonProperty
    private JobId jobId;

    @JsonProperty
    private JobStatus newStatus;


    /**
     * @deprecated Used by JSON deserializer.
     */
    public JobEvent() {}

    public JobEvent(JobId jobId, JobStatus newStatus) {
        this.jobId = jobId;
        this.newStatus = newStatus;
    }


    public JobId getJobId() {
        return jobId;
    }

    public JobStatus getNewStatus() {
        return newStatus;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobEvent that = (JobEvent) o;

        if (jobId != null ? !jobId.equals(that.jobId) : that.jobId != null) return false;
        return newStatus == that.newStatus;

    }

    @Override
    public int hashCode() {
        int result = jobId != null ? jobId.hashCode() : 0;
        result = 31 * result + (newStatus != null ? newStatus.hashCode() : 0);
        return result;
    }
}
