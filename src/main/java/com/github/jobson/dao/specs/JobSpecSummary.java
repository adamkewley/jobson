/*
 * Copyright (c) 2017 Adam Kewley
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.github.jobson.dao.specs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jobson.api.v1.JobSpecId;
import io.swagger.annotations.ApiModelProperty;

public class JobSpecSummary {

    @ApiModelProperty(value = "A unique identifier for the job spec being summarized.", example = "akewley")
    @JsonProperty
    private JobSpecId id;

    @ApiModelProperty(value = "Name of the job spec", example = "Echo")
    @JsonProperty
    private String name;

    @ApiModelProperty(value = "Human-readable description of the job spec", example = "Echoes whatever text is provided via the input")
    @JsonProperty
    private String description;


    public JobSpecSummary() {}

    public JobSpecSummary(
            JobSpecId id,
            String name,
            String description) {

        this.id = id;
        this.name = name;
        this.description = description;
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobSpecSummary that = (JobSpecSummary) o;

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
