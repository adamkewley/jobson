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
import com.fasterxml.jackson.databind.JsonNode;
import com.github.jobson.jobinputs.JobExpectedInputId;
import com.github.jobson.specs.JobSpecId;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Schema(description = "A job request")
public final class APIJobRequest {

    @Schema(description = "The job spec that the request is being made against")
    @JsonProperty
    @NotNull
    private JobSpecId spec;

    @Schema(description = "Human-readable name for the job")
    @JsonProperty
    @NotNull
    private String name;

    @Schema(description = "Inputs for the job")
    @JsonProperty
    @NotNull
    private Map<JobExpectedInputId, JsonNode> inputs;


    /**
     * @deprecated Used by JSON deserializer.
     */
    public APIJobRequest() {}

    public APIJobRequest(
            JobSpecId spec,
            String name,
            Map<JobExpectedInputId, JsonNode> inputs) {
        this.spec = spec;
        this.name = name;
        this.inputs = inputs;
    }


    public JobSpecId getSpec() {
        return spec;
    }

    public String getName() {
        return name;
    }

    public Map<JobExpectedInputId, JsonNode> getInputs() {
        return inputs;
    }
}
