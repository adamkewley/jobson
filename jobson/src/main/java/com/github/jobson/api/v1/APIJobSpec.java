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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jobson.jobinputs.JobExpectedInput;
import com.github.jobson.specs.JobSpec;
import com.github.jobson.specs.JobSpecId;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "A job spec's details")
public final class APIJobSpec {

    public static APIJobSpec fromJobSpec(JobSpec jobSpec) {
        return new APIJobSpec(
                jobSpec.getId(),
                jobSpec.getName(),
                jobSpec.getDescription(),
                jobSpec.getExpectedInputs());
    }


    @Schema(description = "ID of the job spec")
    @JsonProperty
    private JobSpecId id;

    @Schema(description = "Human-readable name of the job spec")
    @JsonProperty
    private String name;

    @Schema(description = "Human-readable description of the job spec")
    @JsonProperty
    private String description;

    @Schema(description = "The inputs clients must provide to spawn to create a job from this spec")
    @JsonProperty
    private List<JobExpectedInput<?>> expectedInputs;


    /**
     * @deprecated Used by JSON deserializer.
     */
    public APIJobSpec() {}

    public APIJobSpec(
            JobSpecId id,
            String name,
            String description,
            List<JobExpectedInput<?>> expectedInputs) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.expectedInputs = expectedInputs;
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

    public List<JobExpectedInput<?>> getExpectedInputs() {
        return expectedInputs;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        APIJobSpec that = (APIJobSpec) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        return expectedInputs != null ? expectedInputs.equals(that.expectedInputs) : that.expectedInputs == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (expectedInputs != null ? expectedInputs.hashCode() : 0);
        return result;
    }
}
