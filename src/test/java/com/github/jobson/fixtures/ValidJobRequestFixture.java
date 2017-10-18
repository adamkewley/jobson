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

package com.github.jobson.fixtures;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.jobson.api.v1.APIJobRequest;
import com.github.jobson.api.v1.UserId;
import com.github.jobson.jobinputs.JobExpectedInputId;
import com.github.jobson.jobs.jobstates.ValidJobRequest;
import com.github.jobson.specs.JobSpec;

import java.util.Map;

public final class ValidJobRequestFixture {

    @JsonProperty
    private String name;

    @JsonProperty
    private UserId owner;

    @JsonProperty
    private Map<JobExpectedInputId, JsonNode> inputs;

    @JsonProperty
    private JobSpec spec;



    public UserId getOwner() {
        return owner;
    }

    public Map<JobExpectedInputId, JsonNode> getInputs() {
        return inputs;
    }

    public JobSpec getSpec() {
        return spec;
    }

    public String getName() {
        return name;
    }



    public ValidJobRequest toValidJobRequest() {
        final APIJobRequest apiJobRequest = new APIJobRequest(
                this.spec.getId(),
                this.name,
                this.inputs);

        return ValidJobRequest.tryCreate(
                this.spec,
                this.owner,
                apiJobRequest).getLeft().get();
    }
}
