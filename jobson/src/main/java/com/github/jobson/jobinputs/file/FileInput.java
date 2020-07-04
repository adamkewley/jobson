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

package com.github.jobson.jobinputs.file;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jobson.jobinputs.JobInput;

import javax.validation.constraints.NotNull;
import java.util.Base64;

public final class FileInput implements JobInput {

    @NotNull
    @JsonProperty
    private final String filename;

    @NotNull
    @JsonProperty
    private final byte[] data;

    @JsonCreator
    public FileInput(@JsonProperty(value = "filename") String filename,
                     @JsonProperty(value = "data", required = true) String b64data) {
        this.filename = filename != null ? filename : "unnamed";
        this.data = Base64.getDecoder().decode(b64data);
    }

    public FileInput(String filename,
                     byte[] b64data) {
        this.filename = filename != null ? filename : "unnamed";
        this.data = b64data;
    }

    public byte[] getData() {
        return this.data;
    }

    public String getFilename() {
        return this.filename;
    }
}
