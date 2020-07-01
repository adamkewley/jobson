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

package com.github.jobson.jobinputs.sql;

import com.github.jobson.utils.ValidationError;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static com.github.jobson.Helpers.toJSON;
import static com.github.jobson.TestHelpers.readJSONFixture;
import static org.assertj.core.api.Assertions.assertThat;

public final class SQLInputSchemaTest {

    @Test
    public void testCanDeserializeFromJson() {
        readJSONFixture(
                "fixtures/jobinputs/sql/example-schema.json",
                SQLExpectedInput.class);
    }

    @Test
    public void testCanSerailizeToJSON() {
        final SQLExpectedInput sqlInputSchema = readJSONFixture(
                "fixtures/jobinputs/sql/example-schema.json",
                SQLExpectedInput.class);

        final String serializedSQLInputSchema = toJSON(sqlInputSchema);
    }

    @Test
    public void testValidateReturnsNoErrorsIfTheInputIsValid() {
        final SQLExpectedInput sqlInputSchema = readJSONFixture(
                "fixtures/jobinputs/sql/1_correct-schema.json",
                SQLExpectedInput.class);

        final SQLInput sqlInput = readJSONFixture(
                "fixtures/jobinputs/sql/1_correct-input.json",
                SQLInput.class);

        final Optional<List<ValidationError>> maybeValidationErrors = sqlInputSchema.validate(sqlInput);

        assertThat(maybeValidationErrors.isPresent()).isFalse();
    }

    @Test
    public void testValidateReturnsErrorsIfAnIncorrectColumnIsRequestedInTheSQL() {
        final SQLExpectedInput sqlInputSchema = readJSONFixture(
                "fixtures/jobinputs/sql/2_correct-schema.json",
                SQLExpectedInput.class);

        final SQLInput sqlInput = readJSONFixture(
                "fixtures/jobinputs/sql/2_incorrect-input.json",
                SQLInput.class);

        final Optional<List<ValidationError>> maybeValidationErrors = sqlInputSchema.validate(sqlInput);

        assertThat(maybeValidationErrors.isPresent()).isTrue();
    }

    @Test
    public void testValidateReturnsErrorsIfInvalidSQLIsProvided() {
        final SQLExpectedInput sqlInputSchema = readJSONFixture(
                "fixtures/jobinputs/sql/3_correct-schema.json",
                SQLExpectedInput.class);

        final SQLInput sqlInput = readJSONFixture(
                "fixtures/jobinputs/sql/3_incorrect-input.json",
                SQLInput.class);

        final Optional<List<ValidationError>> maybeValidationErrors = sqlInputSchema.validate(sqlInput);

        assertThat(maybeValidationErrors.isPresent()).isTrue();
    }
}