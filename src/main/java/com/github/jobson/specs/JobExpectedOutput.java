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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class JobExpectedOutput {

    @JsonProperty
    @NotNull
    private RawTemplateString id;

    @JsonProperty
    @NotNull
    private RawTemplateString path;

    @JsonProperty
    private Optional<String> mimeType = Optional.empty();

    @JsonProperty
    private Optional<String> name = Optional.empty();

    @JsonProperty
    private Optional<String> description = Optional.empty();

    @JsonProperty
    private Map<String, String> metadata = new HashMap<>();

    @JsonProperty
    private boolean required = false;


    /**
     * @deprecated Used by JSON deserializer.
     */
    public JobExpectedOutput() {}

    public JobExpectedOutput(
            RawTemplateString id,
            RawTemplateString path,
            String mimeType) {
        this.id = id;
        this.path = path;
        this.mimeType = Optional.of(mimeType);
    }

    public JobExpectedOutput(
            RawTemplateString id,
            RawTemplateString path,
            String mimeType,
            Optional<String> name,
            Optional<String> description,
            Map<String, String> metadata) {
        this.id = id;
        this.path = path;
        this.mimeType = Optional.of(mimeType);
        this.name = name;
        this.description = description;
        this.metadata = metadata;
    }

    public JobExpectedOutput(
            RawTemplateString id,
            RawTemplateString path,
            Optional<String> mimeType,
            Optional<String> name,
            Optional<String> description,
            Map<String, String> metadata,
            boolean required) {
        this.id = id;
        this.path = path;
        this.mimeType = mimeType;
        this.name = name;
        this.description = description;
        this.metadata = metadata;
        this.required = required;
    }

    public RawTemplateString getId() {
        return id;
    }

    public RawTemplateString getPath() {
        return path;
    }

    public Optional<String> getMimeType() {
        return mimeType;
    }

    public Optional<String> getName() {
        return name;
    }

    public Optional<String> getDescription() {
        return description;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public boolean isRequired() {
        return required;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobExpectedOutput that = (JobExpectedOutput) o;
        return required == that.required &&
                Objects.equals(id, that.id) &&
                Objects.equals(path, that.path) &&
                Objects.equals(mimeType, that.mimeType) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, path, mimeType, name, description, metadata, required);
    }
}
