/*
 * Copyright (c) 2017 Adam Kewley
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.github.jobson.dao.jobs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jobson.jobs.JobId;
import com.github.jobson.jobs.JobStatus;
import com.github.jobson.jobs.JobTimestamp;
import com.github.jobson.api.v1.UserId;
import com.github.jobson.jobs.jobstates.PersistedJob;
import io.swagger.annotations.ApiModelProperty;

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


    @ApiModelProperty(value = "The job's ID")
    @JsonProperty
    private JobId id;

    @ApiModelProperty(value = "A name for the job")
    @JsonProperty
    private String name;

    @ApiModelProperty(value = "The owner of the job.")
    @JsonProperty
    private UserId owner;

    @ApiModelProperty(value = "Timestamps indicating when job status changes occurred")
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
