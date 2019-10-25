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

package com.github.jobson.jobinputs.select;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Schema(description = "An input schema for an option. An option is one of the available choices exposed by an options schema.")
public final class SelectOption {

    @Schema(description = "The ID of the schema", example = "json")
    @JsonProperty
    @NotNull
    private String id;

    @Schema(description = "The display name for the option", example = "JSON")
    @JsonProperty
    @NotNull
    private String name;

    @Schema(description = "A description of the option.",
            example = "JavaScript Object Notation (JSON) is a plaintext format for representing objects, arrays, and primitive values.")
    @JsonProperty
    private Optional<String> description = Optional.empty();



    /**
     * @deprecated Used by JSON deserializer.
     */
    public SelectOption() {}

    public SelectOption(
            String id,
            String name,
            Optional<String> description) {

        this.id = id;
        this.name = name;
        this.description = description;
    }



    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Optional<String> getDescription() {
        return description;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SelectOption that = (SelectOption) o;

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
