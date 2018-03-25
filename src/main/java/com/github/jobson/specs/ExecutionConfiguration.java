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

import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public final class ExecutionConfiguration {

    @JsonProperty
    @NotNull
    private String application;

    @JsonProperty
    private Optional<List<RawTemplateString>> arguments;

    @JsonProperty
    private Optional<List<JobDependencyConfiguration>> dependencies = Optional.empty();


    /**
     * @deprecated Used by JSON deserializer
     */
    public ExecutionConfiguration() {}

    public ExecutionConfiguration(
            String application,
            Optional<List<RawTemplateString>> arguments,
            Optional<List<JobDependencyConfiguration>> dependencies) {

        this.application = application;
        this.arguments = arguments;
        this.dependencies = dependencies;
    }


    public String getApplication() {
        return application;
    }

    public Optional<List<RawTemplateString>> getArguments() {
        return arguments;
    }

    public Optional<List<JobDependencyConfiguration>> getDependencies() {
        return dependencies;
    }


    public ExecutionConfiguration withDependenciesResolvedRelativeTo(Path p) {
        final Optional<List<JobDependencyConfiguration>> resolvedJobDependencies =
                dependencies.map(dependencies ->
                        dependencies.stream().map(
                                dependency -> dependency.withSourceResolvedRelativeTo(p))
                                .collect(toList()));

        return new ExecutionConfiguration(application, arguments, resolvedJobDependencies);
    }

    public ExecutionConfiguration withDependencies(List<JobDependencyConfiguration> dependencies) {
        return new ExecutionConfiguration(this.application, this.arguments, Optional.of(dependencies));
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExecutionConfiguration that = (ExecutionConfiguration) o;

        if (application != null ? !application.equals(that.application) : that.application != null) return false;
        if (arguments != null ? !arguments.equals(that.arguments) : that.arguments != null) return false;
        return dependencies != null ? dependencies.equals(that.dependencies) : that.dependencies == null;

    }

    @Override
    public int hashCode() {
        int result = application != null ? application.hashCode() : 0;
        result = 31 * result + (arguments != null ? arguments.hashCode() : 0);
        result = 31 * result + (dependencies != null ? dependencies.hashCode() : 0);
        return result;
    }
}
