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

package com.github.jobson.jobinputs.stringarray;

import com.github.javafaker.Faker;
import com.github.jobson.jobinputs.JobExpectedInput;
import com.github.jobson.jobinputs.JobExpectedInputId;
import com.github.jobson.utils.ValidationError;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Schema(description = "Schema for an input that requires clients to provide multiple values")
public final class StringArrayExpectedInput extends JobExpectedInput<StringArrayInput> {

    /**
     * @deprecated Used by JSON deserializer.
     */
    public StringArrayExpectedInput() {}

    public StringArrayExpectedInput(
            JobExpectedInputId id,
            String name,
            String description,
            Optional<StringArrayInput> defaultValue) {

        super(id, name, description, defaultValue);
    }


    @Override
    public Class<StringArrayInput> getExpectedInputClass() {
        return StringArrayInput.class;
    }

    @Override
    public Optional<List<ValidationError>> validate(StringArrayInput input) {
        return Optional.empty();
    }

    @Override
    public StringArrayInput generateExampleInput() {
        final Faker f = new Faker();

        final List<String> items =
                Stream.generate(() -> f.lorem().fixedString(7))
                        .limit(4)
                        .collect(toList());

        return new StringArrayInput(items);
    }
}
