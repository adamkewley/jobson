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

package com.github.jobson.api.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.*;

@ApiModel(description = "Details of a job on the system")
public class JobDetailsResponse {

    @ApiModelProperty(value = "The job's ID")
    @JsonProperty
    private JobId id;

    @ApiModelProperty(value = "A name for the job")
    @JsonProperty
    private String name;

    @ApiModelProperty(value = "The owner of the job.")
    @JsonProperty
    private UserSummary owner;

    @ApiModelProperty(value = "The spec that the job was made against")
    @JsonProperty
    private JobSpecSummary jobSpec;

    @ApiModelProperty(value = "Timestamps indicating when job status changes occurred")
    @JsonProperty
    private List<JobStatusChangeTimestamp> statusChanges;

    @ApiModelProperty(value = "Links to related resources and actions")
    @JsonProperty
    private Map<String, RESTLink> _links;



    /**
     * @deprecated Used by JSON deserializer
     */
    public JobDetailsResponse() {}

    public JobDetailsResponse(
            JobId id,
            String name,
            UserSummary owner,
            JobSpecSummary jobSpec,
            List<JobStatusChangeTimestamp> statusChanges,
            Map<String, RESTLink> _links) {

        this.id = id;
        this.owner = owner;
        this.jobSpec = jobSpec;
        this.statusChanges = statusChanges;
        this.name = name;
        this._links = _links;
    }



    public JobId getId() {
        return id;
    }

    public UserSummary getOwner() {
        return owner;
    }

    public JobSpecSummary getJobSpec() {
        return jobSpec;
    }

    public List<JobStatusChangeTimestamp> getStatusChanges() {
        return statusChanges;
    }

    public String getName() {
        return name;
    }

    @JsonIgnore
    public Map<String, RESTLink> getLinks() {
        return _links;
    }



    public JobDetailsResponse withStatusChangeTimestamp(JobStatusChangeTimestamp jobStatusChangeTimestamp) {
        final List<JobStatusChangeTimestamp> newTimestamps = new ArrayList<>();
        newTimestamps.addAll(statusChanges);
        newTimestamps.add(jobStatusChangeTimestamp);

        return new JobDetailsResponse(id, name, owner, jobSpec, newTimestamps, _links);
    }

    public JobSummary toSummary() {
        return new JobSummary(
                id,
                owner,
                latestStatus(),
                name,
                new HashMap<>());
    }

    public Date firstStatusDate() {
        return this.statusChanges.get(0).getTime();
    }

    public JobStatus latestStatus() {
        return this.getStatusChanges().get(this.getStatusChanges().size() - 1).getStatus();
    }
}
