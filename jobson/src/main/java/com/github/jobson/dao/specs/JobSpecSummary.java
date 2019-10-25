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

package com.github.jobson.dao.specs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jobson.specs.JobSpecId;
import io.swagger.v3.oas.annotations.media.Schema;

public class JobSpecSummary {

    @Schema(description = "A unique identifier for the job spec being summarized.", example = "akewley")
    @JsonProperty
    private JobSpecId id;

    @Schema(description = "Name of the job spec", example = "Echo")
    @JsonProperty
    private String name;

    @Schema(description = "Human-readable description of the job spec", example = "Echoes whatever text is provided via the input")
    @JsonProperty
    private String description;


    public JobSpecSummary() {}

    public JobSpecSummary(
            JobSpecId id,
            String name,
            String description) {

        this.id = id;
        this.name = name;
        this.description = description;
    }


    public JobSpecId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobSpecSummary that = (JobSpecSummary) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return description != null ? description.equals(that.description) : that.description == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
