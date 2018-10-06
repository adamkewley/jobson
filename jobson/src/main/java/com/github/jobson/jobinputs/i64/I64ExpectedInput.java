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

import com.github.jobson.jobinputs.JobExpectedInput;
import com.github.jobson.utils.ValidationError;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class I64ExpectedInput extends JobExpectedInput<I64Input> {
    @Override
    public Class<I64Input> getExpectedInputClass() {
        return I64Input.class;
    }

    @Override
    public Optional<List<ValidationError>> validate(I64Input input) {
        return Optional.empty();
    }

    @Override
    public I64Input generateExampleInput() {
        final long value = new Random().nextLong();
        return new I64Input(value);
    }
}
