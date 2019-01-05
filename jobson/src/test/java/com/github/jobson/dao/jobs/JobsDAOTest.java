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

package com.github.jobson.dao.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jobson.Helpers;
import com.github.jobson.TestHelpers;
import com.github.jobson.jobinputs.JobExpectedInputId;
import com.github.jobson.jobs.JobId;
import com.github.jobson.jobs.JobOutput;
import com.github.jobson.jobs.JobStatus;
import com.github.jobson.jobs.JobTimestamp;
import com.github.jobson.jobs.jobstates.ValidJobRequest;
import com.github.jobson.specs.JobOutputId;
import com.github.jobson.utils.BinaryData;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.jobson.Constants.JOB_TIMESTAMP_RESOLUTION_IN_MILLISECONDS;
import static com.github.jobson.Helpers.*;
import static com.github.jobson.TestHelpers.*;
import static com.github.jobson.jobs.JobStatus.FINISHED;
import static com.github.jobson.jobs.JobStatus.RUNNING;
import static com.google.common.collect.Lists.reverse;
import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.*;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class JobsDAOTest {

    protected abstract JobDAO getInstance();


    @Test
    public void testPersistNewJobReturnsPersistedRequestForValidRequest() throws IOException {
        assertThat(getInstance().persist(STANDARD_VALID_REQUEST)).isNotNull();
    }

    @Test
    public void testGetJobDetailsByIdReturnsEmptyForNotPersistedJob() throws IOException {
        assertThat(getInstance().getJobDetailsById(generateJobId())).isNotPresent();
    }

    @Test
    public void testGetJobDetailsByIdReturnsOptionalOfJobForAPersistedJob() throws IOException {
        final JobDAO dao = getInstance();
        final JobId id = dao.persist(STANDARD_VALID_REQUEST).getId();
        assertThat(dao.getJobDetailsById(id)).isPresent();
    }



    @Test
    public void testJobExistsReturnsTrueForAPersistedJob() throws IOException {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        assertThat(dao.jobExists(jobId));
    }

    @Test
    public void testJobExistsReturnsFalseForNotPersistedJob() throws IOException {
        final JobDAO dao = getInstance();
        assertThat(dao.jobExists(generateJobId())).isFalse();
    }



    @Test(expected = IllegalArgumentException.class)
    public void testGetJobsThrowsIllegalArgumentExceptionIfPageSizeIsNegative() throws IOException {
        final JobDAO dao =  getInstance();
        dao.getJobs(-1, 20);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetJobsThrowsIllegalArgumentExceptionIfPageIsNegative() throws IOException {
        final JobDAO dao = getInstance();
        dao.getJobs(0, -10);
    }

    @Test
    public void testGetJobsReturnsAList() throws IOException {
        final JobDAO dao = getInstance();
        assertThat(dao.getJobs(5, 0)).isNotNull();
    }

    @Test
    public void testGetJobsReturnsTheNewestJobsFirst() throws IOException, InterruptedException {
        final JobDAO dao = getInstance();
        final List<JobId> jobIdsInCreationOrder = new ArrayList<>();
        final int numJobsToGenerate = randomIntBetween(10, 30);

        for (int i = 0; i < numJobsToGenerate; i++) {
            jobIdsInCreationOrder.add(dao.persist(STANDARD_VALID_REQUEST).getId());
            sleep(JOB_TIMESTAMP_RESOLUTION_IN_MILLISECONDS * 2);
        }

        final List<JobId> jobSummariesReturned = dao
                .getJobs(numJobsToGenerate, 0)
                .stream()
                .map(JobDetails::getId)
                .collect(toList());

        assertThat(jobSummariesReturned).isEqualTo(reverse(jobIdsInCreationOrder));
    }

    @Test
    public void testGetJobsReturnsNoMoreThanTheSpecifiedPageSize() throws IOException {
        final JobDAO dao = getInstance();
        final int pageSize = 10;
        final int numJobsToGenerate = pageSize * 2;

        for (int i = 0; i < numJobsToGenerate; i++) {
            dao.persist(STANDARD_VALID_REQUEST);
        }

        assertThat(dao.getJobs(pageSize, 0).size()).isEqualTo(pageSize);
    }

    @Test
    public void testGetJobsReturnsNothingIfPageAndPageSizeGoesBeyondTheResultSize() throws IOException {
        final JobDAO dao = getInstance();
        assertThat(dao.getJobs(50, 50).size()).isEqualTo(0);
    }



    @Test(expected = IllegalArgumentException.class)
    public void testGetJobsWithQueryStringThrowsIfPageSizeIsNegative() throws IOException {
        final JobDAO dao = getInstance();
        dao.getJobs(-1, 20, generateRandomString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetJobsWithQueryStringThrowsIfPageIsNegative() throws IOException {
        final JobDAO dao = getInstance();
        dao.getJobs(20, -1, generateRandomString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetJobsWithQueryStringThrowsIfTheQueryIsNull() throws IOException {
        final JobDAO dao = getInstance();
        dao.getJobs(20, 10, null);
    }

    @Test
    public void testGetJobsWithQueryStringReturnsAList() throws IOException {
        final JobDAO dao = getInstance();

        assertThat(dao.getJobs(10, 10, generateRandomString()))
                .isNotNull();
    }

    @Test
    public void testGetJobsWithAQueryStringReturnsSubmittedJobs() throws IOException {
        final JobDAO dao = getInstance();
        final Set<JobId> expectedJobIds = new HashSet<>();
        final int numJobsToGenerate = 20;

        for (int i = 0; i < numJobsToGenerate; i++) {
            expectedJobIds.add(dao.persist(STANDARD_VALID_REQUEST).getId());
        }

        final Set<JobId> returnedJobIds = dao
                .getJobs(numJobsToGenerate, 0, "")
                .stream()
                .map(JobDetails::getId)
                .collect(toSet());

        assertThat(returnedJobIds).isEqualTo(expectedJobIds);
    }

    @Test
    public void testGetJobsWithQueryStringContainingNameReturnsOnlyJobsWithThatName() throws IOException {
        final JobDAO dao = getInstance();
        final List<ValidJobRequest> reqs =
                Stream.generate(TestHelpers::generateRandomString)
                        .map(TestHelpers::validRequestWithName)
                        .limit(randomIntBetween(5, 50))
                        .collect(toList());
        final int numJobRequests = randomIntBetween(20, 50);
        final Map<ValidJobRequest, Set<JobId>> responses = new HashMap<>();

        for (int i = 0; i < numJobRequests; i++) {
            final int idx = randomIntBetween(0, reqs.size() - 1);
            final ValidJobRequest req = reqs.get(idx);
            final JobId id = dao.persist(req).getId();

            if (responses.containsKey(req)) {
                responses.get(req).add(id);
            } else {
                final Set<JobId> s = new HashSet<>();
                s.add(id);
                responses.put(req, s);
            }
        }

        final ValidJobRequest reqBeingQueried = randomKeyIn(responses);
        final Set<JobId> expectedIds = responses.get(reqBeingQueried);

        final Set<JobId> returnedIds =
                dao.getJobs(numJobRequests, 0, reqBeingQueried.getName())
                        .stream()
                        .map(JobDetails::getId)
                        .collect(toSet());

        assertThat(returnedIds).isEqualTo(expectedIds);
    }

    @Test
    public void testGetJobsWithQueryStringContainingSubstringOfNameReturnsOnlyJobsWithThatSubstring() throws IOException {
        final JobDAO dao = getInstance();
        final List<ValidJobRequest> reqs =
                Stream.generate(() -> validRequestWithName(generateRandomString()))
                        .limit(randomIntBetween(5, 50))
                        .collect(toList());
        final int numJobRequests = TestHelpers.randomIntBetween(20, 50);
        final Map<ValidJobRequest, Set<JobId>> responses = new HashMap<>();

        for (int i = 0; i < numJobRequests; i++) {
            final int idx = randomIntBetween(0, reqs.size() - 1);
            final ValidJobRequest req = reqs.get(idx);
            final JobId id = dao.persist(req).getId();

            if (responses.containsKey(req)) {
                responses.get(req).add(id);
            } else {
                final Set<JobId> s = new HashSet<>();
                s.add(id);
                responses.put(req, s);
            }
        }

        final ValidJobRequest reqBeingQueried = randomKeyIn(responses);
        final Set<JobId> expectedIds = responses.get(reqBeingQueried);
        final String query = randomSubstring(reqBeingQueried.getName(), 7);

        final Set<JobId> returnedIds =
                dao.getJobs(numJobRequests, 0, query)
                        .stream()
                        .map(JobDetails::getId)
                        .collect(toSet());

        assertThat(returnedIds).isEqualTo(expectedIds);
    }

    @Test
    public void testGetJobsWithQueryStringContainingAuthorNameReturnsJobsWithThatAuthorName() throws IOException {
        final JobDAO dao = getInstance();
        final ValidJobRequest firstRequest = validRequestWithOwner(generateUserId());
        final ValidJobRequest secondRequest = validRequestWithOwner(generateUserId());
        final int numJobRequests = 20;

        final Map<ValidJobRequest, Set<JobId>> allJobIds =
                IntStream.range(0, numJobRequests)
                        .mapToObj(i -> i % 2 == 0 ? firstRequest : secondRequest)
                        .map(req -> Pair.of(req, dao.persist(req).getId()))
                        .collect(groupingBy(Pair::getLeft, mapping(Pair::getRight, toSet())));

        final String query = firstRequest.getOwner().toString();

        final Set<JobId> returnedJobIds =
                dao.getJobs(numJobRequests, 0, query)
                        .stream()
                        .map(JobDetails::getId)
                        .collect(Collectors.toSet());

        final Set<JobId> expectedJobIds = allJobIds.get(firstRequest);

        assertThat(returnedJobIds).isEqualTo(expectedJobIds);
    }

    @Test
    public void testGetJobSummariesWithQueryStringContainingJobIdReturnsTheJobWithThatId() throws IOException {
        final JobDAO dao = getInstance();
        final ValidJobRequest request = TestHelpers.validRequestWithOwner(TestHelpers.generateUserId());

        final int numJobRequests = 20;

        final Set<JobId> jobIds = new HashSet<>(numJobRequests);
        for(int i = 0; i < numJobRequests; i++) {
            jobIds.add(dao.persist(request).getId());
        }

        for (JobId jobId : jobIds) {
            final Set<JobId> returnedIds = dao
                    .getJobs(numJobRequests, 0, jobId.toString())
                    .stream()
                    .map(JobDetails::getId)
                    .collect(Collectors.toSet());

            assertThat(returnedIds).contains(jobId);
        }
    }



    @Test
    public void testHasStdoutReturnsFalseForNonExistentJob() throws IOException {
        final JobDAO dao = getInstance();
        assertThat(dao.hasStdout(generateJobId())).isFalse();
    }

    @Test
    public void testHasStdoutReturnsFalseForPersistedJobBeforeCallingPersistStdout() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        assertThat(dao.hasStdout(jobId)).isFalse();
    }

    @Test
    public void testHasStdoutReturnsTrueForPersistedJobAfterCallingPersistStdout() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        dao.appendStdout(jobId, TestHelpers.generateRandomByteObservable());

        assertThat(dao.hasStdout(jobId));
    }



    @Test
    public void testHasStderrReturnsFalseForNonExistentJob() throws IOException {
        final JobDAO dao = getInstance();
        assertThat(dao.hasStderr(generateJobId())).isFalse();
    }

    @Test
    public void testHasStderrReturnsFalseForPersistedJobBeforeCallingPersistStderr() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        assertThat(dao.hasStderr(jobId)).isFalse();
    }

    @Test
    public void testHasStderrReturnsTrueForPersistedJobAfterCallingPersistStderr() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        dao.appendStderr(jobId, TestHelpers.generateRandomByteObservable());

        assertThat(dao.hasStderr(jobId));
    }



    @Test
    public void testGetStdoutReturnsEmptyForNonExistentJob() {
        final JobDAO dao = getInstance();
        assertThat(dao.getStdout(generateJobId())).isNotPresent();
    }

    @Test
    public void testGetStdoutReturnsEmptyForPersistedJobBeforeCallingPersistStdout() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        assertThat(dao.getStdout(jobId)).isNotPresent();
    }

    @Test
    public void testGetStdoutReturnsOptionalOfStdoutAfterCallingPersistStdout() throws IOException {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        final byte[] suppliedData = TestHelpers.generateRandomBytes();
        dao.appendStdout(jobId, Observable.just(suppliedData));
        final byte[] returnedData = toByteArray(dao.getStdout(jobId).get().getData());

        assertThat(returnedData).isEqualTo(suppliedData);
    }



    @Test
    public void testGetStderrReturnsEmptyForNonExistentJob() {
        final JobDAO dao = getInstance();
        assertThat(dao.getStderr(generateJobId())).isNotPresent();
    }

    @Test
    public void testGetStderrReturnsEmptyForPersistedJobBeforeCallingPersistStderr() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        assertThat(dao.getStderr(jobId)).isNotPresent();
    }

    @Test
    public void testGetStderrReturnsOptionalOfStderrAfterCallingPersistStderr() throws IOException {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        final byte[] suppliedData = TestHelpers.generateRandomBytes();
        dao.appendStderr(jobId, Observable.just(suppliedData));
        final byte[] returnedData = toByteArray(dao.getStderr(jobId).get().getData());

        assertThat(returnedData).isEqualTo(suppliedData);
    }


    @Test(expected = RuntimeException.class)
    public void testPersistStdoutThrowsForNonExistentJob() {
        final JobDAO dao = getInstance();
        dao.appendStdout(generateJobId(), TestHelpers.generateRandomByteObservable());
    }

    @Test
    public void testPersistStdoutSubscribesToTheObservable() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        final AtomicBoolean daoSubscribedToStdout = new AtomicBoolean(false);
        final Observable<byte[]> stdout = Observable.create(subscriber -> daoSubscribedToStdout.set(true));

        dao.appendStdout(jobId, stdout);

        assertThat(daoSubscribedToStdout.get());
    }

    @Test
    public void testPersistStdoutReadsDataFromObservable() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        final Subject<byte[]> stdoutSubject = PublishSubject.create();
        final AtomicBoolean stdoutObsWasRead = new AtomicBoolean(false);
        final Observable<byte[]> stdoutObs = stdoutSubject.map(data -> {
            stdoutObsWasRead.set(true);
            return data;
        });

        dao.appendStdout(jobId, stdoutObs);

        assertThat(stdoutObsWasRead.get());
    }

    @Test
    public void testPersistStdoutReturnsADisposableThatStopsFurtherReads() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        final Subject<byte[]> stdoutSubject = PublishSubject.create();
        final AtomicBoolean stdoutObsWasRead = new AtomicBoolean(false);
        final Observable<byte[]> stdoutObs = stdoutSubject.map(data -> {
            stdoutObsWasRead.set(true);
            return data;
        });

        final Disposable disposable = dao.appendStdout(jobId, stdoutObs);
        disposable.dispose();
        stdoutSubject.onNext(TestHelpers.generateRandomBytes());

        assertThat(stdoutObsWasRead.get());
    }



    @Test(expected = RuntimeException.class)
    public void testPersistStderrThrowsForNonExistentJob() {
        final JobDAO dao = getInstance();
        dao.appendStderr(generateJobId(), TestHelpers.generateRandomByteObservable());
    }

    @Test
    public void testPersistStderrSubscribesToTheObservable() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        final AtomicBoolean daoSubscribedToStderr = new AtomicBoolean(false);
        final Observable<byte[]> stderr = Observable.create(subscriber -> daoSubscribedToStderr.set(true));

        dao.appendStderr(jobId, stderr);

        assertThat(daoSubscribedToStderr.get());
    }

    @Test
    public void testPersistStderrReadsDataFromObservable() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        final Subject<byte[]> stderrSubject = PublishSubject.create();
        final AtomicBoolean stderrObsWasRead = new AtomicBoolean(false);
        final Observable<byte[]> stderrObs = stderrSubject.map(data -> {
            stderrObsWasRead.set(true);
            return data;
        });

        dao.appendStderr(jobId, stderrObs);

        assertThat(stderrObsWasRead.get());
    }

    @Test
    public void testPersistStderrReturnsADisposableThatStopsFurtherReads() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        final Subject<byte[]> stderrSubject = PublishSubject.create();
        final AtomicBoolean stderrObsWasRead = new AtomicBoolean(false);
        final Observable<byte[]> stderrObs = stderrSubject.map(data -> {
            stderrObsWasRead.set(true);
            return data;
        });

        final Disposable disposable = dao.appendStderr(jobId, stderrObs);
        disposable.dispose();
        stderrSubject.onNext(TestHelpers.generateRandomBytes());

        assertThat(stderrObsWasRead.get());
    }



    @Test(expected = RuntimeException.class)
    public void testAddNewJobStatusThrowsForNonExistentJob() {
        final JobDAO dao = getInstance();
        dao.addNewJobStatus(generateJobId(), TestHelpers.generateJobStatus(), generateRandomString());
    }

    @Test
    public void testAddNewJobStatusAddsTheJobStatusToTheEndOfTheStatusChangesList() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        final JobStatus newStatus = TestHelpers.generateJobStatus();
        final String newStatusMessage = generateRandomString();

        dao.addNewJobStatus(jobId, newStatus, newStatusMessage);

        final Optional<JobTimestamp> last =
                dao.getJobDetailsById(jobId)
                        .map(JobDetails::getTimestamps)
                        .flatMap(Helpers::lastElement);

        assertThat(last).isPresent();
        assertThat(last.get().getMessage().get()).isEqualTo(newStatusMessage);
        assertThat(last.get().getStatus()).isEqualTo(newStatus);
    }


    @Test
    public void testGetJobInputsReturnsEmptyIfJobDoesNotExist() {
        final JobDAO dao = getInstance();

        assertThat(dao.getJobInputs(generateJobId())).isNotPresent();
    }

    @Test
    public void testGetJobInputsReturnsTheSuppliedInputsIfTheJobDoesExist() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        final Optional<Map<JobExpectedInputId, JsonNode>> maybeInputs =
                dao.getJobInputs(jobId);
        final Map<JobExpectedInputId, JsonNode> expectedInptus =
                mapValues(STANDARD_VALID_REQUEST.getInputs(), Helpers::toJSONNode);

        assertThat(maybeInputs.isPresent());
        assertThat(maybeInputs.get()).isEqualTo(expectedInptus);
    }


    @Test
    public void testGetJobsWithStatusReturnsExpectedJobs() {
        final JobDAO dao = getInstance();
        final Map<JobStatus, Set<JobId>> expectedResults =
                IntStream.range(1, TestHelpers.randomIntBetween(10, 100))
                        .mapToObj(i -> TestHelpers.randomFloat() < 0.5f ? FINISHED : RUNNING)
                        .map(status -> {
                            final JobId id = dao.persist(STANDARD_VALID_REQUEST).getId();
                            dao.addNewJobStatus(id, status, generateRandomString());
                            return Pair.of(status, id);
                        })
                        .collect(groupingBy(Pair::getLeft, mapping(Pair::getRight, toSet())));

        expectedResults.forEach((status, expectedIds) -> {
            final Set<JobId> returnedIds = dao.getJobsWithStatus(status);
            assertThat(returnedIds).isEqualTo(expectedIds);
        });
    }


    @Test
    public void testHasOutputReturnsFalseIfNoOutputPersisted() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();

        assertThat(dao.hasOutput(jobId, generateJobOutputId())).isFalse();
    }

    @Test
    public void testHasOutputReturnsTrueIfOutputIsPersisted() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();

        final JobOutput jobOutput = generateRandomJobOutput();

        dao.persistOutput(jobId, jobOutput);

        assertThat(dao.hasOutput(jobId, jobOutput.getId())).isTrue();
    }

    @Test
    public void testGetOutputReturnsPersistedOutput() throws IOException {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();

        final byte inputData[] = generateRandomBytes();
        final JobOutput jobOutput = generateRandomJobOutput(inputData);

        dao.persistOutput(jobId, jobOutput);

        final Optional<BinaryData> maybeRet = dao.getOutput(jobId, jobOutput.getId());

        assertThat(maybeRet).isPresent();

        final byte[] returnedData = IOUtils.toByteArray(maybeRet.get().getData());

        assertThat(returnedData).isEqualTo(inputData);
    }


    @Test
    public void testGetJobOutputsReturnsEmptyListForNonExistentJob() {
        final JobDAO dao = getInstance();

        final List<JobOutputDetails> returnedJobOutputs = dao.getJobOutputs(generateJobId());

        assertThat(returnedJobOutputs.size()).isEqualTo(0);
    }

    @Test
    public void testGetJobOutputsReturnsEmptyListForExistentJobWithOutputs() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();

        final Set<JobOutputId> persistedOutputs = new HashSet<>();
        final int numberOfFilesToPersist = randomIntBetween(5, 15);

        for (int i = 0; i < numberOfFilesToPersist; i++) {
            final JobOutput jobOutput = generateRandomJobOutput();
            dao.persistOutput(jobId, jobOutput);
            persistedOutputs.add(jobOutput.getId());
        }

        final Set<JobOutputId> returnedIds =
                dao.getJobOutputs(jobId).stream().map(JobOutputDetails::getId).collect(Collectors.toSet());

        assertThat(returnedIds).isEqualTo(persistedOutputs);
    }

    @Test
    public void testHasJobInputsReturnsFalseIfJobDoesNotExist() {
        final JobDAO dao = getInstance();
        assertThat(dao.hasJobInputs(generateJobId())).isFalse();
    }

    @Test
    public void testHasJobInputsReturnsTrueIfJobExists() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        assertThat(dao.hasJobInputs(jobId)).isTrue();
    }

    @Test
    public void testRemoveRemovesAJob() {
        final JobDAO dao = getInstance();
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();

        assertThat(dao.jobExists(jobId)).isTrue();

        dao.remove(jobId);

        assertThat(dao.jobExists(jobId)).isFalse();
    }

    @Test
    public void testRemoveFailsSilentlyIfTryingToRemoveAJobThatDoesntExist() {
        final JobDAO dao = getInstance();

        // Shouldn't throw
        dao.remove(generateJobId());
    }
}
