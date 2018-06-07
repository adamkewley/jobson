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

package com.github.jobson.specs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jobson.dao.specs.JobSpecSummary;
import com.github.jobson.jobinputs.JobExpectedInput;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JobSpec {

    @JsonProperty
    @NotNull
    private JobSpecId id;

    @JsonProperty
    @NotNull
    private String name;

    @JsonProperty
    @NotNull
    private String description;

    @JsonProperty
    @Valid
    private List<JobExpectedInput<?>> expectedInputs = new ArrayList<>();

    @JsonProperty
    @NotNull
    @Valid
    private ExecutionConfiguration execution;

    @JsonProperty
    @Valid
    private List<JobExpectedOutput> expectedOutputs = new ArrayList<>();


    /**
     * @deprecated Used by JSON serializer
     */
    public JobSpec() {}

    public JobSpec(
            JobSpecId id,
            String name,
            String description,
            List<JobExpectedInput<?>> expectedInputs,
            ExecutionConfiguration execution) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.expectedInputs = expectedInputs;
        this.execution = execution;
    }

    public JobSpec(
            JobSpecId id,
            String name,
            String description,
            List<JobExpectedInput<?>> expectedInputs,
            ExecutionConfiguration execution,
            List<JobExpectedOutput> expectedOutputs) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.expectedInputs = expectedInputs;
        this.execution = execution;
        this.expectedOutputs = expectedOutputs;
    }


    public JobSpecId getId() {
        return id;
    }

    public void setId(JobSpecId id) {
        this.id = id;
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

    public ExecutionConfiguration getExecution() {
        return execution;
    }

    public List<JobExpectedOutput> getExpectedOutputs() {
        return expectedOutputs;
    }


    public JobSpecSummary toSummary() {
        return new JobSpecSummary(this.getId(), this.getName(), this.getDescription());
    }

    public JobSpec withDependenciesResolvedRelativeTo(Path p) {
        return new JobSpec(
                id,
                name,
                description,
                expectedInputs,
                execution.withDependenciesResolvedRelativeTo(p),
                expectedOutputs);
    }

    public JobSpec withExecutionConfiguration(ExecutionConfiguration executionConfig) {
        return new JobSpec(id, name, description, expectedInputs, executionConfig, expectedOutputs);
    }

    public JobSpec withExpectedOutputs(List<JobExpectedOutput> expectedOutputs) {
        return new JobSpec(id, name, description, expectedInputs, execution, expectedOutputs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobSpec jobSpec = (JobSpec) o;

        if (id != null ? !id.equals(jobSpec.id) : jobSpec.id != null) return false;
        if (name != null ? !name.equals(jobSpec.name) : jobSpec.name != null) return false;
        if (description != null ? !description.equals(jobSpec.description) : jobSpec.description != null) return false;
        if (expectedInputs != null ? !expectedInputs.equals(jobSpec.expectedInputs) : jobSpec.expectedInputs != null)
            return false;
        if (execution != null ? !execution.equals(jobSpec.execution) : jobSpec.execution != null) return false;
        return expectedOutputs != null ? expectedOutputs.equals(jobSpec.expectedOutputs) : jobSpec.expectedOutputs == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (expectedInputs != null ? expectedInputs.hashCode() : 0);
        result = 31 * result + (execution != null ? execution.hashCode() : 0);
        result = 31 * result + (expectedOutputs != null ? expectedOutputs.hashCode() : 0);
        return result;
    }
}
