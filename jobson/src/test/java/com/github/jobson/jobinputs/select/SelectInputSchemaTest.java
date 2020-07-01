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

package com.github.jobson.jobinputs.select;

import com.github.jobson.Helpers;
import com.github.jobson.TestHelpers;
import com.github.jobson.utils.ValidationError;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


public final class SelectInputSchemaTest {

    @Test
    public void testCanDeserializeFromJSON() {
        // Shouldn't throw
        TestHelpers.readJSONFixture(
                "fixtures/jobinputs/select/example-schema.json",
                SelectExpectedInput.class);
    }

    @Test
    public void testCanSerializeToJSON() {
        // I'm using this to spot-check what jackson actually produces
        // when serializing JSON
        final SelectExpectedInput deserializedSchema = TestHelpers.readJSONFixture(
                "fixtures/jobinputs/select/example-schema.json",
                SelectExpectedInput.class);

        final String serialziedSchemaJSON = Helpers.toJSON(deserializedSchema);
    }

    @Test
    public void testValidateReturnsNoErrorsForAValidInputAgainstTheSchema() {
        final SelectExpectedInput validSchema = TestHelpers.readJSONFixture(
                "fixtures/jobinputs/select/1_correct-schema.json",
                SelectExpectedInput.class);

        final SelectInput validOptionsInput = TestHelpers.readJSONFixture(
                "fixtures/jobinputs/select/1_correct-input.json",
                SelectInput.class);

        final Optional<List<ValidationError>> maybeValidationErrors = validSchema.validate(validOptionsInput);

        assertThat(maybeValidationErrors.isPresent()).isFalse();
    }

    @Test
    public void testValidateReturnsErrorsIfTheSelectedOptionIsntInTheSchema() {
        final SelectExpectedInput validSchema = TestHelpers.readJSONFixture(
                "fixtures/jobinputs/select/2_correct-schema.json",
                SelectExpectedInput.class);

        final SelectInput invalidOptionsInput = TestHelpers.readJSONFixture(
                "fixtures/jobinputs/select/2_incorrect-input.json",
                SelectInput.class);

        final Optional<List<ValidationError>> maybeValidationErrors = validSchema.validate(invalidOptionsInput);

        assertThat(maybeValidationErrors.isPresent()).isTrue();
    }
}