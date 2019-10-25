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
import com.github.jobson.dao.specs.JobSpecSummary;
import com.github.jobson.specs.JobSpecId;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Summary of a job spec")
public class APIJobSpecSummary extends JobSpecSummary {

    public static APIJobSpecSummary fromJobSpecSummary(
            JobSpecSummary summary,
            Map<String, APIRestLink> restLinks) {

        return new APIJobSpecSummary(
                summary.getId(),
                summary.getName(),
                summary.getDescription(),
                restLinks);
    }


    @Schema(description = "Links to related resources and actions")
    @JsonProperty
    private Map<String, APIRestLink> _links;


    /**
     * @deprecated Used by JSON deserializer
     */
    public APIJobSpecSummary() {}

    public APIJobSpecSummary(
            JobSpecId id,
            String name,
            String description,
            Map<String, APIRestLink> _links) {
        super(id, name, description);
        this._links = _links;
    }


    @JsonIgnore
    public Map<String, APIRestLink> getLinks() {
        return _links;
    }


    public JobSpecSummary toJobSpecSummary() {
        return new JobSpecSummary(getId(), getName(), getDescription());
    }
}
