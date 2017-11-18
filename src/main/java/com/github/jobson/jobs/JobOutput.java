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

package com.github.jobson.jobs;

import com.github.jobson.specs.JobOutputId;
import com.github.jobson.utils.BinaryData;

import java.util.Map;
import java.util.Optional;

public final class JobOutput {

    private JobOutputId id;
    private BinaryData data;
    private Optional<String> name;
    private Optional<String> description;
    private Map<String, String> metadata;


    public JobOutput(
            JobOutputId id,
            BinaryData data,
            Optional<String> name,
            Optional<String> description,
            Map<String, String> metadata) {
        this.id = id;
        this.data = data;
        this.name = name;
        this.description = description;
        this.metadata = metadata;
    }


    public JobOutputId getId() {
        return id;
    }

    public BinaryData getData() {
        return data;
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
