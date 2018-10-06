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

package com.github.jobson.jobinputs.f64;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.jobson.jobinputs.JobInput;

import javax.validation.constraints.NotNull;

public final class F64Input implements JobInput {

    @NotNull
    private final double value;


    @JsonCreator
    public F64Input(long value) {
        this.value = value;
    }

    @JsonCreator
    public F64Input(double value) {
        this.value = value;
    }

    @JsonCreator
    public F64Input(String value) {
        // The API may have to accept 64-bit doubles in string format because
        // javascript clients can't handle 64-bit binary decimals without losing
        // precision (after 16 significant digits, "real" doubles lose precision
        // after 21 digits). Because of this, clients *may* send a double as a string
        // which the server parses into a binary double server-side
        this.value = Double.parseDouble(value);
    }

    @Override
    @JsonValue
    public String toString() {
        return Double.toString(value);
    }
}
