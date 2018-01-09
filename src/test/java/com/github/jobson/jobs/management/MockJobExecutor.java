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

import com.github.jobson.jobs.JobEventListeners;
import com.github.jobson.jobs.JobExecutionResult;
import com.github.jobson.jobs.JobExecutor;
import com.github.jobson.jobs.JobStatus;
import com.github.jobson.jobs.jobstates.PersistedJob;
import com.github.jobson.utils.CancelablePromise;
import com.github.jobson.utils.SimpleCancelablePromise;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.util.function.Supplier;

import static com.github.jobson.TestHelpers.generateRandomBytes;

public final class MockJobExecutor implements JobExecutor {

    public static MockJobExecutor thatResolvesWith(JobExecutionResult result) {
        return thatResolvesWith(result, generateRandomBytes(), generateRandomBytes());
    }

    public static MockJobExecutor thatResolvesWith(JobExecutionResult result, byte[] stdout, byte[] stderr) {
        final CancelablePromise<JobExecutionResult> ret = new SimpleCancelablePromise<>();
        ret.complete(result);
        return new MockJobExecutor(() -> ret, Observable.just(stdout), Observable.just(stderr));
    }

    public static MockJobExecutor thatUses(CancelablePromise<JobExecutionResult> promise) {
        return thatUses(promise, Observable.just(generateRandomBytes()), Observable.just(generateRandomBytes()));
    }

    public static MockJobExecutor thatUses(Observable<byte[]> stdout, Observable<byte[]> stderr) {
        final CancelablePromise<JobExecutionResult> p = new SimpleCancelablePromise<>();
        p.complete(new JobExecutionResult(JobStatus.FINISHED));
        return new MockJobExecutor(() -> p, stdout, stderr);
    }

    public static MockJobExecutor thatUses(CancelablePromise<JobExecutionResult> promise, Observable<byte[]> stdout, Observable<byte[]> stderr) {
        return new MockJobExecutor(() -> promise, stdout, stderr);
    }

    public static MockJobExecutor thatUses(Supplier<CancelablePromise<JobExecutionResult>> promiseSupplier) {
        return new MockJobExecutor(promiseSupplier, Observable.just(generateRandomBytes()), Observable.just(generateRandomBytes()));
    }


    private final Subject<PersistedJob> executionCalls = PublishSubject.create();
    private final Supplier<CancelablePromise<JobExecutionResult>> promiseSupplier;
    private final Observable<byte[]> stdout;
    private final Observable<byte[]> stderr;


    public MockJobExecutor(
            Supplier<CancelablePromise<JobExecutionResult>> promiseSupplier,
            Observable<byte[]> stdout,
            Observable<byte[]> stderr) {

        this.promiseSupplier = promiseSupplier;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    @Override
    public CancelablePromise<JobExecutionResult> execute(PersistedJob persistedJob, JobEventListeners jobEventListeners) {
        stdout.subscribe(jobEventListeners.getOnStdoutListener());
        stderr.subscribe(jobEventListeners.getOnStderrListener());
        this.executionCalls.onNext(persistedJob);
        return this.promiseSupplier.get();
    }

    public Observable<PersistedJob> getExecutionCalls() {
        return this.executionCalls;
    }
}
