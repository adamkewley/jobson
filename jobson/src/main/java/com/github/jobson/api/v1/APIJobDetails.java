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
import com.github.jobson.dao.jobs.JobDetails;
import com.github.jobson.jobs.JobId;
import com.github.jobson.jobs.JobTimestamp;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Details of a job on the system")
public class APIJobDetails extends JobDetails {

    public static APIJobDetails fromJobDetails(
            JobDetails jobDetails,
            Map<String, APIRestLink> restLinks) {

        return new APIJobDetails(
                jobDetails.getId(),
                jobDetails.getName(),
                jobDetails.getOwner(),
                jobDetails.getTimestamps(),
                restLinks);
    }


    @Schema(description = "Links to related resources and actions")
    @JsonProperty
    private Map<String, APIRestLink> _links;


    /**
     * @deprecated Used by JSON deserializer
     */
    public APIJobDetails() {}

    public APIJobDetails(
            JobId id,
            String name,
            UserId owner,
            List<JobTimestamp> timestamps,
            Map<String, APIRestLink> _links) {

        super(id, name, owner, timestamps);
        this._links = _links;
    }


    @JsonIgnore
    public Map<String, APIRestLink> getLinks() {
        return _links;
    }
}
