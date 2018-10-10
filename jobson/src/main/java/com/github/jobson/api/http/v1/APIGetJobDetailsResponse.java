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

package com.github.jobson.api.http.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

@ApiModel(description = "Details of a job on the system")
public class APIGetJobDetailsResponse {

    @ApiModelProperty(value = "The job's ID")
    @JsonProperty
    private String id;

    @ApiModelProperty(value = "A name for the job")
    @JsonProperty
    private String name;

    @ApiModelProperty(value = "The owner of the job.")
    @JsonProperty
    private APIUserId owner;

    @ApiModelProperty(value = "Current status of the job")
    @JsonProperty
    private APIJobStatus status;

    @ApiModelProperty(value = "Timestamps indicating when job status changes occurred")
    @JsonProperty
    private List<APIJobTimestamp> timestamps;

    @ApiModelProperty(value = "Links to related resources and actions")
    @JsonProperty
    private Map<String, APIRestLink> _links;


    public APIGetJobDetailsResponse(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("owner") APIUserId owner,
            @JsonProperty("status") APIJobStatus status,
            @JsonProperty("timestamps") List<APIJobTimestamp> timestamps,
            @JsonProperty("_links") Map<String, APIRestLink> _links) {

        this.id = id;
        this.name = name;
        this.owner = owner;
        this.status = status;
        this.timestamps = timestamps;
        this._links = _links;
    }


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public APIUserId getOwner() {
        return owner;
    }

    public APIJobStatus getStatus() {
        return status;
    }

    public List<APIJobTimestamp> getTimestamps() {
        return timestamps;
    }

    @JsonIgnore
    public APIJobStatus latestStatus() {
        return this.timestamps.get(this.timestamps.size() - 1).getStatus();
    }

    @JsonIgnore
    public Map<String, APIRestLink> getLinks() {
        return _links;
    }
}
