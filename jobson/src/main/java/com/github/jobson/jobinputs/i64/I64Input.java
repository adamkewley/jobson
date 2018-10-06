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

package com.github.jobson.jobinputs.i64;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.jobson.jobinputs.JobInput;

import javax.validation.constraints.NotNull;

public final class I64Input implements JobInput {

    @NotNull
    private long value;

    @JsonCreator
    public I64Input(long value) {
        this.value = value;
    }

    @JsonCreator
    public I64Input(String value) {
        // The API may have to accept 64-bit numbers in string format because
        // javascript itself can't handle 64-bit binary numbers, so they're
        // sent as strings and parsed server-side.
        this.value = Long.parseLong(value);
    }

    @Override
    @JsonValue
    public String toString() {
        return Long.toString(value);
    }
}
