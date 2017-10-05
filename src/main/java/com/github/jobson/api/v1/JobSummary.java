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

import java.util.Map;

@ApiModel(description = "Summary information for a job")
public final class JobSummary {

    @ApiModelProperty(value = "A unique identifier for the job")
    @JsonProperty
    private JobId id;

    @ApiModelProperty(value = "The owner of the job")
    @JsonProperty
    private UserSummary owner;

    @ApiModelProperty(value = "The current status of the job")
    @JsonProperty
    private JobStatus status;

    @ApiModelProperty(value = "A description of the job")
    @JsonProperty
    private String description;

    @ApiModelProperty(value = "Links to related resources and actions")
    @JsonProperty
    private Map<String, RESTLink> _links;



    /**
     * @deprecated Used by JSON deserializer
     */
    public JobSummary() {}

    public JobSummary(
            JobId id,
            UserSummary owner,
            JobStatus status,
            String description,
            Map<String, RESTLink> _links) {

        this.id = id;
        this.owner = owner;
        this.status = status;
        this.description = description;
        this._links = _links;
    }



    public JobId getId() {
        return id;
    }

    public UserSummary getOwner() {
        return owner;
    }

    public JobStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    @JsonIgnore
    public Map<String, RESTLink> getLinks() {
        return this._links;
    }
}
