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

package com.github.jobson.dao.jobs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jobson.api.v1.UserId;
import com.github.jobson.jobs.JobId;
import com.github.jobson.jobs.JobStatus;
import com.github.jobson.jobs.JobTimestamp;
import com.github.jobson.jobs.jobstates.PersistedJob;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

public class JobDetails {

    public static JobDetails fromPersistedJob(PersistedJob persistedJob) {
        return new JobDetails(
                persistedJob.getId(),
                persistedJob.getName(),
                persistedJob.getOwner(),
                persistedJob.getTimestamps());
    }


    @Schema(description = "The job's ID")
    @JsonProperty
    private JobId id;

    @Schema(description = "A name for the job")
    @JsonProperty
    private String name;

    @Schema(description = "The owner of the job.")
    @JsonProperty
    private UserId owner;

    @Schema(description = "Timestamps indicating when job status changes occurred")
    @JsonProperty
    private List<JobTimestamp> timestamps;


    /**
     * @deprecated Used by JSON deserializer
     */
    public JobDetails() {}

    public JobDetails(JobId id, String name, UserId owner, List<JobTimestamp> timestamps) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.timestamps = timestamps;
    }


    public JobId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UserId getOwner() {
        return owner;
    }

    public List<JobTimestamp> getTimestamps() {
        return timestamps;
    }


    public JobDetails withStatusChangeTimestamp(JobTimestamp jobTimestamp) {
        final List<JobTimestamp> newTimestamps = new ArrayList<>();
        newTimestamps.addAll(timestamps);
        newTimestamps.add(jobTimestamp);

        return new JobDetails(id, name, owner, newTimestamps);
    }

    public JobStatus latestStatus() {
        return latestStatusChange().getStatus();
    }

    private JobTimestamp latestStatusChange() {
        return this.getTimestamps().get(this.getTimestamps().size() - 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobDetails that = (JobDetails) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (owner != null ? !owner.equals(that.owner) : that.owner != null) return false;
        return timestamps != null ? timestamps.equals(that.timestamps) : that.timestamps == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (timestamps != null ? timestamps.hashCode() : 0);
        return result;
    }
}
