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
package com.github.jobson.execution.finalizing;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class LocalJobFinalizerTest {

    @Test
    public void testFinalizeRemovesJobWorkingDirectoryIfRemoveWorkingDirectoriesIsEnabled() {
        // TODO: No functionality for this yet.
        assertThat(false).isTrue();
    }

    @Test
    public void testFinalizeCopiesOutputsWithAbsolutePathsCorrectly() {
        assertThat(false).isTrue();
    }

    @Test
    public void testFinalizeDoesntThrowIfUsingAnAbsoluteExpectedOutputThatDoesntExist() {
        assertThat(false).isTrue();
    }

    @Test
    public void testFinalizeDoesntThrowIfUsingAnAbsoluteExpectedOutputThatDoesntExistANDTheWorkingDirectoryIsRelative() {
        assertThat(false).isTrue();
    }



    // FROM JOB MANAGER

    /*

    @Test
    public void testSubmitPersistsJobOutputsAfterExecution() throws InterruptedException, ExecutionException, TimeoutException {
        final CancelablePromise<JobExecutionResult> executorPromise = new SimpleCancelablePromise<>();
        final MockInMemoryJobWriter writingJobDAO = new MockInMemoryJobWriter();

        final JobManager jobManager =
                createManagerWith(writingJobDAO, MockJobExecutor.thatUses(executorPromise));

        final byte[] executorOutputBytes = generateRandomBytes();
        final List<JobOutputResult> outputsFromExecutor = new ArrayList<>();

        for (JobExpectedOutput expectedOutput : STANDARD_VALID_REQUEST.getSpec().getExpectedOutputs()) {
            if (expectedOutput.getMimeType().isPresent()) {
                outputsFromExecutor.add(new JobOutput(
                        new JobOutputId(expectedOutput.getId().toString()),
                        wrap(executorOutputBytes, expectedOutput.getMimeType().get()),
                        expectedOutput.getName(),
                        expectedOutput.getDescription(),
                        expectedOutput.getMetadata()));
            } else {
                outputsFromExecutor.add(new JobOutput(
                        new JobOutputId(expectedOutput.getId().toString()),
                        wrap(executorOutputBytes),
                        expectedOutput.getName(),
                        expectedOutput.getDescription(),
                        expectedOutput.getMetadata()));
            }
        }

        final JobExecutionResult jobExecutionResult = new JobExecutionResult(FINISHED, outputsFromExecutor);

        final CancelablePromise<FinalizedJob> p = jobManager.submit(STANDARD_VALID_REQUEST).getRight();

        executorPromise.complete(jobExecutionResult);

        p.get(DEFAULT_TIMEOUT, MILLISECONDS);

        for (JobExpectedOutput output : STANDARD_VALID_REQUEST.getSpec().getExpectedOutputs()) {
            final PersistOutputArgs expectedArgs = new PersistOutputArgs(
                    writingJobDAO.getReturnedPersistedReq().getId(),
                    new JobOutputId(output.getId().toString()),
                    wrap(executorOutputBytes, output.getMimeType().orElse("application/octet-stream")));

            assertThat(writingJobDAO.getPersistOutputCalledWith()).contains(expectedArgs);
        }
    }

    @Test
    public void testFailsIfARequiredOutputDoesNotExistInExecutionResult() throws InterruptedException, ExecutionException, TimeoutException {
        final CancelablePromise<JobExecutionResult> executorPromise = new SimpleCancelablePromise<>();
        final MockInMemoryJobWriter writingJobDAO = new MockInMemoryJobWriter();
        final JobManager jobManager = createManagerWith(writingJobDAO, MockJobExecutor.thatUses(executorPromise));

        final String jobOutputId = "some-id";
        final String jobOutputPath = "some-non-existent-path";

        final JobExpectedOutput expectedOutput = new JobExpectedOutput(
                new RawTemplateString(jobOutputId),
                new RawTemplateString("some-non-existent-path"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Collections.emptyMap(),
                true);

        final JobSpec jobSpec = STANDARD_VALID_REQUEST.getSpec().
                withExpectedOutputs(Collections.singletonList(expectedOutput));
        final ValidJobRequest jobRequest = STANDARD_VALID_REQUEST.withSpec(jobSpec);


        final JobExecutionResult result = new JobExecutionResult(
                JobStatus.FINISHED,
                Collections.singletonList(new MissingOutput(
                        new JobOutputId(jobOutputId),
                        true,
                        jobOutputPath
                )));
        final Pair<JobId, CancelablePromise<FinalizedJob>> submissionReturn = jobManager.submit(jobRequest);
        final JobId jobId = submissionReturn.getLeft();
        final CancelablePromise<FinalizedJob> p = submissionReturn.getRight();
        executorPromise.complete(result);


        final FinalizedJob finalJobState = p.get();
        final List<AddNewJobStatusArgs> addNewJobStatusArgs = writingJobDAO.getAddNewJobStatusArgsCalledWith();

        assertThat(finalJobState.getFinalStatus()).isEqualTo(JobStatus.FATAL_ERROR);

        final boolean anyStatusUpdateContainsMissingOutput =
                addNewJobStatusArgs.stream()
                        .map(AddNewJobStatusArgs::getStatusMessage)
                        .anyMatch(msg -> msg.toLowerCase().contains("missing required output"));

        assertThat(anyStatusUpdateContainsMissingOutput);
    }

    */
}
