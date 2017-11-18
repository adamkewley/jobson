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

package com.github.jobson.api.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jobson.dao.jobs.JobOutputDetails;
import com.github.jobson.jobs.JobOutput;
import com.github.jobson.specs.JobExpectedOutput;
import com.github.jobson.specs.JobOutputId;
import io.swagger.annotations.ApiModel;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.github.jobson.Constants.HTTP_JOBS_PATH;

@ApiModel(description = "Details about an output produced by a job")
public final class APIJobOutput {

    public static APIJobOutput fromJobOutput(
            String outputsFolderHref,
            JobOutputDetails jobOutputDetails) {

        return new APIJobOutput(
                jobOutputDetails.getId(),
                jobOutputDetails.getSizeInBytes(),
                outputsFolderHref,
                jobOutputDetails.getMimeType(),
                jobOutputDetails.getName(),
                jobOutputDetails.getDescription(),
                jobOutputDetails.getMetadata());
    }


    @JsonProperty
    @NotNull
    private JobOutputId id;

    @JsonProperty
    private long sizeInBytes;

    @JsonProperty
    @NotNull
    private String href;

    @JsonProperty
    @NotNull
    private Optional<String> mimeType = Optional.empty();

    @JsonProperty
    private Optional<String> name = Optional.empty();

    @JsonProperty
    private Optional<String> description = Optional.empty();

    @JsonProperty
    private Map<String, String> metadata = new HashMap<>();


    /**
     * @deprecated Used by JSON deserializer.
     */
    public APIJobOutput() {}

    public APIJobOutput(
            JobOutputId id,
            long sizeInBytes,
            String href,
            Optional<String> mimeType,
            Optional<String> name,
            Optional<String> description,
            Map<String, String> metadata) {
        this.id = id;
        this.sizeInBytes = sizeInBytes;
        this.href = href;
        this.mimeType = mimeType;
        this.name = name;
        this.description = description;
        this.metadata = metadata;
    }


    public JobOutputId getId() {
        return this.id;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public String getHref() {
        return href;
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
}
