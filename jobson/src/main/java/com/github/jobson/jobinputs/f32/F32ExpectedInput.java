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

package com.github.jobson.jobinputs.f32;

import com.github.jobson.jobinputs.JobExpectedInput;
import com.github.jobson.utils.ValidationError;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class F32ExpectedInput extends JobExpectedInput<F32Input> {
    @Override
    public Class<F32Input> getExpectedInputClass() {
        return F32Input.class;
    }

    @Override
    public Optional<List<ValidationError>> validate(F32Input input) {
        return Optional.empty();
    }

    @Override
    public F32Input generateExampleInput() {
        final float val = new Random().nextFloat();
        return new F32Input(val);
    }
}
