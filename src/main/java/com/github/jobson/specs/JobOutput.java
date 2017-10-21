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

package com.github.jobson.specs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jobson.Helpers;
import org.apache.tika.Tika;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import static com.github.jobson.Helpers.getMimeTypeFromFilename;

public final class JobOutput {

    @JsonProperty
    @NotNull
    private String path;

    @JsonProperty
    private String mimeType;


    /**
     * @deprecated Used by JSON deserializer.
     */
    public JobOutput() {}

    public JobOutput(String path, String mimeType) {
        this.path = path;
        this.mimeType = mimeType;
    }


    public String getPath() {
        return path;
    }

    public String getMimeType() {
        return mimeType != null ?
                mimeType :
                getMimeTypeFromFilename(path);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobOutput jobOutput = (JobOutput) o;

        if (path != null ? !path.equals(jobOutput.path) : jobOutput.path != null) return false;
        return mimeType != null ? mimeType.equals(jobOutput.mimeType) : jobOutput.mimeType == null;
    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (mimeType != null ? mimeType.hashCode() : 0);
        return result;
    }
}
