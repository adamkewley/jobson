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

package com.github.jobson.jobs.jobstates;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.github.jobson.Helpers;
import com.github.jobson.api.v1.APIJobRequest;
import com.github.jobson.api.v1.UserId;
import com.github.jobson.jobinputs.JobExpectedInput;
import com.github.jobson.jobinputs.JobExpectedInputId;
import com.github.jobson.jobinputs.JobInput;
import com.github.jobson.specs.JobSpec;
import com.github.jobson.utils.Either;
import com.github.jobson.utils.ValidationError;

import java.io.IOException;
import java.util.*;

import static com.github.jobson.Helpers.readJSON;
import static com.google.common.collect.Sets.difference;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

/**
 * Used internally after the API resolves a job request against a user and spec.
 * Cannot be created unless all inputs are resolved & validated.
 */
public class ValidJobRequest {

    public static Either<ValidJobRequest, List<ValidationError>> tryCreate(
            JobSpec jobSpec,
            UserId userId,
            APIJobRequest APIJobRequest) throws RuntimeException {

        final Map<JobExpectedInputId, JobExpectedInput> jobExpectedInputs =
                jobSpec.getExpectedInputs()
                        .stream()
                        .collect(toMap(JobExpectedInput::getId, identity()));

        return resolveJobInputs(jobExpectedInputs, APIJobRequest.getInputs())
                .leftMap(inputs -> new ValidJobRequest(
                        userId,
                        APIJobRequest.getName(),
                        inputs,
                        jobSpec))
                .leftFlatMap(req -> {
                    final List<ValidationError> validationErrors = findValidationErrors(req);

                    if (validationErrors.size() == 0) return Either.left(req);
                    else return Either.right(validationErrors);
                });
    }

    private static Either<Map<JobExpectedInputId, JobInput>, List<ValidationError>> resolveJobInputs(
            Map<JobExpectedInputId, JobExpectedInput> expectedInputs,
            Map<JobExpectedInputId, JsonNode> suppliedInputs) {

        final Map<JobExpectedInputId, JobInput> defaultedInputs =
                resolveDefaultedInputs(expectedInputs, suppliedInputs);

        final Map<JobExpectedInputId, Either<JobInput, ValidationError>> maybeResolvedInputs =
                tryResolveInputs(expectedInputs, suppliedInputs);

        final List<ValidationError> validationErrors = new ArrayList<>();
        final Map<JobExpectedInputId, JobInput> validResolvedInputs = new HashMap<>();

        maybeResolvedInputs.forEach((key, value) ->
                value.handleBoth(
                        jobInput -> validResolvedInputs.put(key, jobInput),
                        validationErrors::add));

        if (validationErrors.size() == 0)
            return Either.left(Helpers.merge(defaultedInputs, validResolvedInputs));
        else return Either.right(validationErrors);
    }

    private static Map<JobExpectedInputId, JobInput> resolveDefaultedInputs(
            Map<JobExpectedInputId, JobExpectedInput> expectedInputs,
            Map<JobExpectedInputId, JsonNode> suppliedInputs) {

        return expectedInputs.entrySet().stream()
                .filter(e -> !suppliedInputs.containsKey(e.getKey()))
                .map(e -> new AbstractMap.SimpleEntry<>(
                        e.getKey(),
                        (Optional<JobInput>)e.getValue().getDefault()))
                .filter(e -> e.getValue().isPresent())
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().get()))
                .collect(toMap(e -> e.getKey(), e -> e.getValue()));
    }

    private static Map<JobExpectedInputId, Either<JobInput, ValidationError>> tryResolveInputs(
            Map<JobExpectedInputId, JobExpectedInput> expectedInputs,
            Map<JobExpectedInputId, JsonNode> suppliedInputs) {

        return suppliedInputs.entrySet().stream().map(entry -> {
            final JobExpectedInputId id = entry.getKey();

            if (!expectedInputs.containsKey(id)) {
                final ValidationError validationError = ValidationError.of(format(
                        "%s: is not an expected input. Expected inputs: %s",
                        id,
                        Helpers.commaSeparatedList(expectedInputs.keySet())));

                return new AbstractMap.SimpleEntry<JobExpectedInputId, Either<JobInput, ValidationError>>(entry.getKey(), Either.right(validationError));
            }

            final JobExpectedInput expectedInput = expectedInputs.get(entry.getKey());
            final Class<JobInput> inputClass = expectedInput.getExpectedInputClass();

            try {
                final JobInput jobInput = readJSON(entry.getValue(), inputClass);

                return new AbstractMap.SimpleEntry<JobExpectedInputId, Either<JobInput, ValidationError>>(entry.getKey(), Either.left(jobInput));
            } catch (UnrecognizedPropertyException ex) {
                final ValidationError validationError = new ValidationError("Unrecognized field \"" + ex.getPropertyName() + "\". Allowed fields for this input: " + ex.getKnownPropertyIds());
                return new AbstractMap.SimpleEntry<JobExpectedInputId, Either<JobInput, ValidationError>>(entry.getKey(), Either.right(validationError));
            } catch (IOException ex) {
                final ValidationError validationError = new ValidationError(ex.toString());
                return new AbstractMap.SimpleEntry<JobExpectedInputId, Either<JobInput, ValidationError>>(entry.getKey(), Either.right(validationError));
            }
        }).collect(toMap(e -> e.getKey(), e -> e.getValue()));
    }

    private static List<ValidationError> findValidationErrors(ValidJobRequest req) {

        final ArrayList<ValidationError> validationErrors = new ArrayList<>();

        validationErrors.addAll(
                findValidationErrorsDueToInputIdNotBeingInSchema(req));
        validationErrors.addAll(
                findValidationErrorsDueToNotSpecifyingARequiredInput(req));

        if (validationErrors.isEmpty()) {
            validationErrors.addAll(
                    findValidationErrorsDueToInputsNotBeingValidAgainstTheirSchema(req));
        }

        return validationErrors;
    }

    private static List<ValidationError> findValidationErrorsDueToInputIdNotBeingInSchema(ValidJobRequest req) {
        final Set<JobExpectedInputId> idsInRequest = req.inputs.keySet();
        final Set<JobExpectedInputId> expectedIds =
                req.spec.getExpectedInputs().stream().map(JobExpectedInput::getId).collect(toSet());

        final Set<JobExpectedInputId> invalidIds = difference(idsInRequest, expectedIds);

        if (invalidIds.isEmpty()) {
            return new ArrayList<>();
        } else {
            final String errorMessage = String.format(
                    "The job request contained unexepected inputs (%s). Available inputs: %s",
                    Helpers.commaSeparatedList(invalidIds),
                    Helpers.commaSeparatedList(expectedIds));

            return singletonList(ValidationError.of(errorMessage));
        }
    }

    private static List<ValidationError> findValidationErrorsDueToNotSpecifyingARequiredInput(ValidJobRequest req) {
        final Set<JobExpectedInputId> requiredInputIds =
                req.spec.getExpectedInputs().stream().map(JobExpectedInput::getId).collect(toSet());
        final Set<JobExpectedInputId> idsInRequest = req.getInputs().keySet();

        final Set<JobExpectedInputId> idsMissingFromRequest = difference(requiredInputIds, idsInRequest);

        if (idsMissingFromRequest.isEmpty()) {
            return new ArrayList<>();
        } else {
            final String errorMessage = "Inputs are missing from the request: " + Helpers.commaSeparatedList(idsMissingFromRequest);
            return singletonList(ValidationError.of(errorMessage));
        }
    }

    private static List<ValidationError> findValidationErrorsDueToInputsNotBeingValidAgainstTheirSchema(ValidJobRequest req) {
        return req.inputs.entrySet()
                .stream()
                .map(entry -> findValidationErrorsDueToAnInputNotBeingValidAgainstItsSchema(req, entry.getKey(), entry.getValue()))
                .flatMap(List::stream)
                .collect(toList());
    }

    private static List<ValidationError> findValidationErrorsDueToAnInputNotBeingValidAgainstItsSchema(
            ValidJobRequest req,
            JobExpectedInputId jobExpectedInputId,
            JobInput input) {

        final Optional<JobExpectedInput<?>> maybeJobSchema =
                req.spec.getExpectedInputs()
                .stream()
                .filter(schema -> schema.getId().equals(jobExpectedInputId))
                .findFirst();

        if (maybeJobSchema.isPresent()) {
            final JobExpectedInput inputSchema = maybeJobSchema.get();

            if (input.getClass().equals(inputSchema.getExpectedInputClass())) {
                final Optional<List<ValidationError>> maybeValidationErrors = inputSchema.validate(input);
                if (maybeValidationErrors.isPresent()) return maybeValidationErrors.get();
                else return emptyList();
            } else {
                final String errorMsg = format(
                        "The input type %s does not match the schema %s",
                        input.getClass().getSimpleName(),
                        inputSchema.getExpectedInputClass().getSimpleName());

                return singletonList(ValidationError.of(errorMsg));
            }
        } else {
            return singletonList(ValidationError.of("Job schema does not contain an input schema with id = " + jobExpectedInputId));
        }
    }



    @JsonProperty
    private UserId owner;

    @JsonProperty
    private String name;

    @JsonProperty
    private Map<JobExpectedInputId, JobInput> inputs;

    @JsonProperty
    private JobSpec spec;


    
    /**
     * @deprecated Used by JSON deserializer
     */
    public ValidJobRequest() {}

    public ValidJobRequest(
            UserId owner,
            String name,
            Map<JobExpectedInputId, JobInput> inputs,
            JobSpec spec) {

        this.owner = owner;
        this.name = name;
        this.inputs = inputs;
        this.spec = spec;
    }



    public UserId getOwner() {
        return owner;
    }

    public String getName() {
        return this.name;
    }

    public Map<JobExpectedInputId, JobInput> getInputs() {
        return this.inputs;
    }

    public JobSpec getSpec() {
        return spec;
    }


    public ValidJobRequest withSpec(JobSpec spec) {
        return new ValidJobRequest(owner, name, inputs, spec);
    }
}
