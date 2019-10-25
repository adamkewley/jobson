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

package com.github.jobson.jobinputs.sql;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;

@Schema(description = "Schema of a column within an sql table")
public final class ColumnSchema {

    @Schema(description = "The ID of the column", example = "TransitId")
    @JsonProperty
    @NotNull
    private String id;

    @Schema(description = "The name of the column", example = "Transit ID")
    @JsonProperty
    @NotNull
    private String name;

    @Schema(description = "A description of the column", example = "The Transit ID of a detector transit")
    @JsonProperty
    @NotNull
    private String description;

    @Schema(description = "A string representing the column's data type. Valid type primitives are 'string' 'int', and " +
                    "'decimal'. Arrays are represented by surrounding a type with brackets; for " +
                    "example '[int]'. An array is also a type, so nesting is permitted. Therefore, " +
                    "'[[string]]' would also a valid type identifier")
    @JsonProperty
    @NotNull
    private String type;



    /**
     * @deprecated Used by JSON deserializer
     */
    public ColumnSchema() {}

    public ColumnSchema(String id, String name, String description, String type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
    }



    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ColumnSchema that = (ColumnSchema) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        return type != null ? type.equals(that.type) : that.type == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
