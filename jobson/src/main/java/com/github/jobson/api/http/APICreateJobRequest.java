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

package com.github.jobson.api.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.util.Map;

@ApiModel(description = "A job request")
public final class APICreateJobRequest {

    @ApiModelProperty(value = "The job spec that the request is being made against")
    @JsonProperty
    @NotNull
    private String spec;

    @ApiModelProperty(value = "Human-readable name for the job")
    @JsonProperty
    @NotNull
    private String name;

    @ApiModelProperty(value = "Inputs for the job")
    @JsonProperty
    @NotNull
    private Map<String, JsonNode> inputs;


    public APICreateJobRequest(
            @JsonProperty("spec") String spec,
            @JsonProperty("name") String name,
            @JsonProperty("inputs") Map<String, JsonNode> inputs) {
        this.spec = spec;
        this.name = name;
        this.inputs = inputs;
    }


    public String getSpec() {
        return spec;
    }

    public String getName() {
        return name;
    }

    public Map<String, JsonNode> getInputs() {
        return inputs;
    }
}
