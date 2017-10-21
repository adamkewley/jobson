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

package com.github.jobson.jobs.management;

import com.codahale.metrics.health.HealthCheck;
import com.github.jobson.Constants;
import com.github.jobson.TestHelpers;
import com.github.jobson.jobs.*;
import com.github.jobson.utils.BinaryData;
import com.github.jobson.dao.jobs.WritingJobDAO;
import com.github.jobson.jobs.jobstates.FinalizedJob;
import com.github.jobson.specs.JobOutput;
import com.github.jobson.utils.CancelablePromise;
import com.github.jobson.utils.SimpleCancelablePromise;
import com.github.jobson.websockets.v1.JobEvent;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;
import io.reactivex.subjects.Subject;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.jobson.Constants.JOB_MANAGER_JOB_QUEUE_OVERFLOW_HEALTHCHECK;
import static com.github.jobson.Constants.JOB_MANAGER_MAX_JOB_QUEUE_OVERFLOW_THRESHOLD;
import static com.github.jobson.TestConstants.DEFAULT_TIMEOUT;
import static com.github.jobson.TestHelpers.STANDARD_VALID_REQUEST;
import static com.github.jobson.TestHelpers.generateRandomBytes;
import static com.github.jobson.jobs.JobStatus.*;
import static com.github.jobson.utils.BinaryData.wrap;
import static com.github.jobson.jobs.JobEventListeners.createNullListeners;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class JobManagerTest {

    private static JobManager createStandardManager() {
        return createManagerWith(
                new MockInMemoryJobWriter(),
                MockJobExecutor.thatResolvesWith(new JobExecutionResult(FINISHED)));
    }

    private static JobManager createManagerWith(JobExecutor jobExecutor) {
        return createManagerWith(new MockInMemoryJobWriter(), jobExecutor);
    }

    private static JobManager createManagerWith(WritingJobDAO dao) {
        return createManagerWith(dao, MockJobExecutor.thatResolvesWith(new JobExecutionResult(FINISHED)));
    }

    private static JobManager createManagerWith(WritingJobDAO dao, JobExecutor executor) {
        return new JobManager(dao, executor, Constants.MAX_RUNNING_JOBS);
    }


    @Test
    public void testGetAllJobStatusChangesReturnsAnObservable() {
        final JobManager jobManager = createStandardManager();
        assertThat(jobManager.allJobStatusChanges()).isNotNull();
    }

    @Test
    public void testGetAllJobStatusChangesObservableEmitsExpectedEvents() throws InterruptedException, ExecutionException, TimeoutException {
        final JobExecutionResult executorResult = new JobExecutionResult(FINISHED);
        final JobExecutor executor = MockJobExecutor.thatResolvesWith(executorResult);
        final JobManager jobManager = createManagerWith(executor);

        final List<JobStatus> statusesEmitted = new ArrayList<>();
        jobManager.allJobStatusChanges()
                .map(JobEvent::getNewStatus)
                .subscribe(statusesEmitted::add);

        final Pair<JobId, CancelablePromise<FinalizedJob>> ret =
                jobManager.submit(STANDARD_VALID_REQUEST);

        ret.getRight().get(DEFAULT_TIMEOUT, MILLISECONDS);

        assertThat(statusesEmitted).isEqualTo(asList(SUBMITTED, RUNNING, FINISHED));
    }

    @Test
    public void testGetAllJobStatusChangesObservableEmitsExpectedEventsWhenError() throws InterruptedException, ExecutionException, TimeoutException {
        final JobExecutionResult executorResult = new JobExecutionResult(FATAL_ERROR);
        final JobExecutor executor = MockJobExecutor.thatResolvesWith(executorResult);
        final JobManager jobManager = createManagerWith(executor);

        final List<JobStatus> statusesEmitted = new ArrayList<>();
        jobManager.allJobStatusChanges()
                .map(JobEvent::getNewStatus)
                .subscribe(statusesEmitted::add);

        final Pair<JobId, CancelablePromise<FinalizedJob>> ret =
                jobManager.submit(STANDARD_VALID_REQUEST);

        ret.getRight().get(DEFAULT_TIMEOUT, MILLISECONDS);

        assertThat(statusesEmitted).isEqualTo(asList(SUBMITTED, RUNNING, FATAL_ERROR));
    }

    @Test
    public void testGetAllJobStatusChangesObservableEmitsExpectedEventsWhenAborted() throws InterruptedException, ExecutionException, TimeoutException {
        final JobExecutionResult executorResult = new JobExecutionResult(ABORTED);
        final JobExecutor executor = MockJobExecutor.thatResolvesWith(executorResult);
        final JobManager jobManager = createManagerWith(executor);

        final List<JobStatus> statusesEmitted = new ArrayList<>();
        jobManager.allJobStatusChanges()
                .map(JobEvent::getNewStatus)
                .subscribe(statusesEmitted::add);

        final Pair<JobId, CancelablePromise<FinalizedJob>> ret =
                jobManager.submit(STANDARD_VALID_REQUEST);

        ret.getRight().get(DEFAULT_TIMEOUT, MILLISECONDS);

        assertThat(statusesEmitted).isEqualTo(asList(SUBMITTED, RUNNING, ABORTED));
    }




    @Test
    public void testGetStdoutUpdatesReturnsEmptyForNonExistentJob() {
        final JobManager jobManager = createStandardManager();
        assertThat(jobManager.stdoutUpdates(TestHelpers.generateJobId())).isNotPresent();
    }

    @Test
    public void testGetStdoutUpdatesReturnsObservableForNotYetFinishedJob() {
        final CancelablePromise<JobExecutionResult> executorPromise = new SimpleCancelablePromise<>();
        final JobExecutor executor = MockJobExecutor.thatUses(executorPromise);
        final JobManager jobManager = createManagerWith(executor);

        final JobId jobId = jobManager.submit(STANDARD_VALID_REQUEST).getLeft();

        assertThat(jobManager.stdoutUpdates(jobId)).isPresent();
    }

    @Test
    public void testGetStdoutUpdatesReturnsEmptyForFinishedJob() {
        final CancelablePromise<JobExecutionResult> executorPromise = new SimpleCancelablePromise<>();
        final JobExecutor executor = MockJobExecutor.thatUses(executorPromise);
        final JobManager jobManager = createManagerWith(executor);

        final JobId jobId = jobManager.submit(STANDARD_VALID_REQUEST).getLeft();

        executorPromise.complete(new JobExecutionResult(FINISHED));

        assertThat(jobManager.stdoutUpdates(jobId)).isNotPresent();
    }

    @Test
    public void testGetStdoutUpdatesEchoesUpdatesFromExecutorObservers() throws InterruptedException, ExecutionException, TimeoutException {
        final CancelablePromise<JobExecutionResult> executorPromise = new SimpleCancelablePromise<>();
        final Subject<byte[]> stdoutSubject = PublishSubject.create();
        final JobExecutor executor =
                MockJobExecutor.thatUses(executorPromise, stdoutSubject, Observable.just(TestHelpers.generateRandomBytes()));
        final JobManager jobManager = createManagerWith(executor);

        final Pair<JobId, CancelablePromise<FinalizedJob>> ret =
                jobManager.submit(STANDARD_VALID_REQUEST);

        final Observable<byte[]> stdoutObservable =
                jobManager.stdoutUpdates(ret.getLeft()).get();

        final AtomicReference<byte[]> bytesFromObservable = new AtomicReference<>();
        stdoutObservable.subscribe(bytesFromObservable::set);

        final byte[] bytesExpected = TestHelpers.generateRandomBytes();
        stdoutSubject.onNext(bytesExpected);

        executorPromise.complete(new JobExecutionResult(FINISHED));

        ret.getRight().get(DEFAULT_TIMEOUT, MILLISECONDS);

        assertThat(bytesFromObservable.get()).isEqualTo(bytesExpected);
    }



    @Test
    public void testGetStderrUpdatesReturnsEmptyForNonExistentJob() {
        final JobManager jobManager = createStandardManager();
        assertThat(jobManager.stderrUpdates(TestHelpers.generateJobId())).isNotPresent();
    }

    @Test
    public void testGetStderrUpdatesReturnsObservableForNotYetFinishedJob() {
        final CancelablePromise<JobExecutionResult> executorPromise = new SimpleCancelablePromise<>();
        final JobExecutor executor = MockJobExecutor.thatUses(executorPromise);
        final JobManager jobManager = createManagerWith(executor);

        final JobId jobId = jobManager.submit(STANDARD_VALID_REQUEST).getLeft();

        assertThat(jobManager.stderrUpdates(jobId)).isPresent();
    }

    @Test
    public void testGetStderrUpdatesReturnsEmptyForFinishedJob() {
        final CancelablePromise<JobExecutionResult> executorPromise = new SimpleCancelablePromise<>();
        final JobExecutor executor = MockJobExecutor.thatUses(executorPromise);
        final JobManager jobManager = createManagerWith(executor);

        final JobId jobId = jobManager.submit(STANDARD_VALID_REQUEST).getLeft();

        executorPromise.complete(new JobExecutionResult(FINISHED));

        assertThat(jobManager.stderrUpdates(jobId)).isNotPresent();
    }

    @Test
    public void testGetStderrUpdatesEchoesUpdatesFromExecutorObservers() throws InterruptedException, ExecutionException, TimeoutException {
        final CancelablePromise<JobExecutionResult> executorPromise = new SimpleCancelablePromise<>();
        final Subject<byte[]> stderrSubject = PublishSubject.create();
        final JobExecutor executor =
                MockJobExecutor.thatUses(executorPromise, Observable.just(TestHelpers.generateRandomBytes()), stderrSubject);
        final JobManager jobManager = createManagerWith(executor);

        final Pair<JobId, CancelablePromise<FinalizedJob>> ret =
                jobManager.submit(STANDARD_VALID_REQUEST);

        final Observable<byte[]> stderrObservable =
                jobManager.stderrUpdates(ret.getLeft()).get();

        final AtomicReference<byte[]> bytesFromObservable = new AtomicReference<>();
        stderrObservable.subscribe(bytesFromObservable::set);

        final byte[] bytesExpected = TestHelpers.generateRandomBytes();
        stderrSubject.onNext(bytesExpected);

        executorPromise.complete(new JobExecutionResult(FINISHED));

        ret.getRight().get(DEFAULT_TIMEOUT, MILLISECONDS);

        assertThat(bytesFromObservable.get()).isEqualTo(bytesExpected);
    }



    // Submit /w listeners

    @Test(expected = NullPointerException.class)
    public void testSubmitThrowsIfArgsNull() {
        final JobManager jobManager = createStandardManager();
        jobManager.submit(null, createNullListeners());
        jobManager.submit(STANDARD_VALID_REQUEST, null);
    }

    @Test
    public void testSubmitReturnsExpectedReturnForValidRequest() throws InterruptedException, ExecutionException, TimeoutException {
        final JobManager jobManager = createStandardManager();
        final Pair<JobId, CancelablePromise<FinalizedJob>> ret =
                jobManager.submit(STANDARD_VALID_REQUEST, createNullListeners());

        assertThat(ret).isNotNull();
        assertThat(ret.getLeft()).isNotNull();
        assertThat(ret.getRight()).isNotNull();
        assertThat(ret.getRight().get(DEFAULT_TIMEOUT, MILLISECONDS)).isNotNull();
    }

    @Test
    public void testSubmitReturnsJobIdProducedByDAO() {
        final MockInMemoryJobWriter dao = new MockInMemoryJobWriter();
        final JobManager jobManager = createManagerWith(dao);
        final JobId ret = jobManager.submit(STANDARD_VALID_REQUEST, createNullListeners()).getLeft();

        assertThat(ret).isEqualTo(dao.getReturnedPersistedReq().getId());
    }

    @Test
    public void testSubmitPromiseResolvesWhenExecutorPromiseResolves() throws InterruptedException, ExecutionException, TimeoutException {
        final CancelablePromise<JobExecutionResult> p = new SimpleCancelablePromise<>();
        final JobExecutor jobExecutor = MockJobExecutor.thatUses(p);
        final JobManager jobManager = createManagerWith(jobExecutor);

        final CancelablePromise<FinalizedJob> ret =
                jobManager.submit(STANDARD_VALID_REQUEST, createNullListeners()).getRight();

        p.complete(JobExecutionResult.fromExitCode(0));

        assertThat(ret.get(DEFAULT_TIMEOUT, MILLISECONDS)).isNotNull();
    }

    @Test
    public void testSubmitJobEventListenersEchoStdoutWhenExecutorEchoesStdout() throws InterruptedException {
        final Subject<byte[]> stdoutSubject = ReplaySubject.create();
        final byte[] expectedStdoutBytes = generateRandomBytes();
        stdoutSubject.onNext(expectedStdoutBytes);

        final JobExecutor jobExecutor = MockJobExecutor.thatUses(stdoutSubject, Observable.never());
        final JobManager jobManager = createManagerWith(jobExecutor);

        final Semaphore s = new Semaphore(1);
        s.acquire();

        final JobEventListeners listeners = JobEventListeners.createStdoutListener(new Observer<byte[]>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull byte[] bytes) {
                assertThat(bytes).isEqualTo(expectedStdoutBytes);
                s.release();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                fail("Error from observable");
                s.release();
           }

            @Override
            public void onComplete() {}
        });

        jobManager.submit(STANDARD_VALID_REQUEST, listeners);

        if (!s.tryAcquire(1, SECONDS)) {
            fail("Timed out before any bytes received");
        }
    }

    @Test
    public void testSubmitJobEventListenersEchoStderrWhenExecutorEchoesStderr() throws InterruptedException {
        final Subject<byte[]> stderr = ReplaySubject.create();
        final byte[] stderrBytes = generateRandomBytes();
        stderr.onNext(stderrBytes);

        final JobExecutor jobExecutor = MockJobExecutor.thatUses(Observable.never(), stderr);
        final JobManager jobManager = createManagerWith(jobExecutor);

        final Semaphore s = new Semaphore(1);
        s.acquire();

        final JobEventListeners listeners = JobEventListeners.createStderrListener(new Observer<byte[]>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull byte[] bytes) {
                assertThat(bytes).isEqualTo(stderrBytes);
                s.release();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                fail("Error from observable");
                s.release();
            }

            @Override
            public void onComplete() {}
        });

        jobManager.submit(STANDARD_VALID_REQUEST, listeners);

        if (!s.tryAcquire(1, SECONDS)) {
            fail("Timed out before any bytes received");
        }
    }


    // Submit w/o listeners



    @Test(expected = NullPointerException.class)
    public void testSubmitThrowsIfJobRequestIsNull() {
        final JobManager jobManager = createStandardManager();
        jobManager.submit(null);
    }

    @Test
    public void testSubmitReturnsExpectedReturnForValidRequest2() throws InterruptedException, ExecutionException, TimeoutException {
        final JobManager jobManager = createStandardManager();
        final Pair<JobId, CancelablePromise<FinalizedJob>> ret = jobManager.submit(STANDARD_VALID_REQUEST);

        assertThat(ret).isNotNull();
        assertThat(ret.getLeft()).isNotNull();
        assertThat(ret.getRight()).isNotNull();
        assertThat(ret.getRight().get(DEFAULT_TIMEOUT, MILLISECONDS)).isNotNull();
    }

    @Test
    public void testSubmitReturnsJobIdProducedByDAO2() {
        final MockInMemoryJobWriter dao = new MockInMemoryJobWriter();
        final JobManager jobManager = createManagerWith(dao);
        final JobId ret = jobManager.submit(STANDARD_VALID_REQUEST).getLeft();

        assertThat(ret).isEqualTo(dao.getReturnedPersistedReq().getId());
    }

    @Test
    public void testSubmitPromiseResolvesWhenExecutorPromiseResolves2() throws InterruptedException, ExecutionException, TimeoutException {
        final CancelablePromise<JobExecutionResult> p = new SimpleCancelablePromise<>();
        final JobExecutor jobExecutor = MockJobExecutor.thatUses(p);
        final JobManager jobManager = createManagerWith(jobExecutor);

        final CancelablePromise<FinalizedJob> ret =
                jobManager.submit(STANDARD_VALID_REQUEST).getRight();

        p.complete(JobExecutionResult.fromExitCode(0));

        assertThat(ret.get(DEFAULT_TIMEOUT, MILLISECONDS)).isNotNull();
    }


    @Test
    public void testTryAbortReturnsFalseForNonExistentJob() {
        final JobManager jobManager = createStandardManager();
        assertThat(jobManager.tryAbort(TestHelpers.generateJobId())).isFalse();
    }

    @Test
    public void testTryAbortReturnsFalseForACompletedJob() throws InterruptedException, ExecutionException, TimeoutException {
        final JobManager jobManager = createStandardManager();

        final Pair<JobId, CancelablePromise<FinalizedJob>> ret =
                jobManager.submit(STANDARD_VALID_REQUEST);

        ret.getRight().get(DEFAULT_TIMEOUT, MILLISECONDS);

        assertThat(jobManager.tryAbort(ret.getLeft())).isFalse();
    }

    @Test
    public void testTryAbortReturnsTrueForARunningJob() {
        final CancelablePromise<JobExecutionResult> executorPromise = new SimpleCancelablePromise<>();
        final JobManager jobManager =
                createManagerWith(MockJobExecutor.thatUses(executorPromise));

        final Pair<JobId, CancelablePromise<FinalizedJob>> ret =
                jobManager.submit(STANDARD_VALID_REQUEST);

        assertThat(jobManager.tryAbort(ret.getLeft())).isTrue();

        // TODO: Check status is aborted.
    }


    @Test
    public void testSubmitPersistsJobOutputsAfterExecution() throws InterruptedException, ExecutionException, TimeoutException {
        final CancelablePromise<JobExecutionResult> executorPromise = new SimpleCancelablePromise<>();
        final MockInMemoryJobWriter writingJobDAO = new MockInMemoryJobWriter();

        final JobManager jobManager =
                createManagerWith(writingJobDAO, MockJobExecutor.thatUses(executorPromise));

        final byte[] executorOutputBytes = generateRandomBytes();
        final Map<String, BinaryData> outputsFromExecutor = new HashMap<>();

        for (Map.Entry<String, JobOutput> output : STANDARD_VALID_REQUEST.getSpec().getOutputs().entrySet()) {
            if (output.getValue().getMimeType().isPresent()) {
                outputsFromExecutor.put(output.getKey(), wrap(executorOutputBytes, output.getValue().getMimeType().get()));
            } else {
                outputsFromExecutor.put(output.getKey(), wrap(executorOutputBytes));
            }
        }

        final JobExecutionResult jobExecutionResult = new JobExecutionResult(FINISHED, outputsFromExecutor);

        final CancelablePromise<FinalizedJob> p = jobManager.submit(STANDARD_VALID_REQUEST).getRight();

        executorPromise.complete(jobExecutionResult);

        p.get(DEFAULT_TIMEOUT, MILLISECONDS);

        for (Map.Entry<String, JobOutput> output : STANDARD_VALID_REQUEST.getSpec().getOutputs().entrySet()) {
            final PersistOutputArgs expectedArgs = new PersistOutputArgs(
                    writingJobDAO.getReturnedPersistedReq().getId(),
                    output.getKey(),
                    wrap(executorOutputBytes, output.getValue().getMimeType().orElse("application/octet-stream")));

            assertThat(writingJobDAO.getPersistOutputCalledWith()).contains(expectedArgs);
        }
    }


    @Test
    public void testGetHealthChecksReturnsAHealthCheckForJobQueueOverflowing() {
        final CancelablePromise<JobExecutionResult> executorPromise = new SimpleCancelablePromise<>();
        final JobManager jobManager = createManagerWith(MockJobExecutor.thatUses(executorPromise));
        final Map<String, HealthCheck> healthChecks = jobManager.getHealthChecks();

        assertThat(healthChecks).containsKeys(JOB_MANAGER_JOB_QUEUE_OVERFLOW_HEALTHCHECK);
        assertThat(healthChecks.get(JOB_MANAGER_JOB_QUEUE_OVERFLOW_HEALTHCHECK)).isNotNull();

        final HealthCheck jobQueueHealthCheck = healthChecks.get(JOB_MANAGER_JOB_QUEUE_OVERFLOW_HEALTHCHECK);

        assertThat(jobQueueHealthCheck.execute().isHealthy());

        for(int i = 0; i < JOB_MANAGER_MAX_JOB_QUEUE_OVERFLOW_THRESHOLD * 2; i++) {
            // These won't finish because we never resolve the promise
            jobManager.submit(STANDARD_VALID_REQUEST);
        }

        assertThat(jobQueueHealthCheck.execute().isHealthy()).isFalse();
    }
}