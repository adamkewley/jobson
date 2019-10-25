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
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Schema of an sql table exposed by an sql expected input")
public final class TableSchema {

    @Schema(description = "The ID of the table", example = "PreprocessedTransits")
    @JsonProperty
    @NotNull
    private String id;

    @Schema(description = "The name of the table", example = "Preprocessed Transits")
    @JsonProperty
    @NotNull
    private String name;

    @Schema(description = "A description of the table", example = "Preprocessed Transits for Segment 1 and 0")
    @JsonProperty
    @NotNull
    private String description;

    @Schema(description = "Columns exposed by the table")
    @JsonProperty
    @NotNull
    @NotEmpty
    private List<ColumnSchema> columns;


    /**
     * @deprecated Used by JSON deserializer.
     */
    public TableSchema() {}

    public TableSchema(
            String id,
            String name,
            String description,
            List<ColumnSchema> columns) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.columns = columns;
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

    public List<ColumnSchema> getColumns() {
        return columns;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TableSchema that = (TableSchema) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        return columns != null ? columns.equals(that.columns) : that.columns == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (columns != null ? columns.hashCode() : 0);
        return result;
    }
}
