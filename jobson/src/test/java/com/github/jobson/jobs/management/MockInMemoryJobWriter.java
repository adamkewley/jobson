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

import com.github.jobson.dao.jobs.WritingJobDAO;
import com.github.jobson.jobs.JobId;
import com.github.jobson.jobs.JobOutput;
import com.github.jobson.jobs.JobStatus;
import com.github.jobson.jobs.jobstates.PersistedJob;
import com.github.jobson.jobs.jobstates.ValidJobRequest;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.jobson.TestHelpers.generateJobId;
import static com.github.jobson.TestHelpers.generateJobStatusChangeTimestamp;
import static java.util.Collections.singletonList;

public final class MockInMemoryJobWriter implements WritingJobDAO {


    private Optional<JobId> persistStdoutCalledWith = Optional.empty();
    private final AtomicBoolean persistStdoutDisposed = new AtomicBoolean(false);
    private Optional<JobId> persistStderrCalledWith = Optional.empty();
    private final AtomicBoolean persistStderrDisposed = new AtomicBoolean(false);
    private Optional<ValidJobRequest> persistCalledWith = Optional.empty();
    private PersistedJob returnedPersistedReq;
    private List<PersistOutputArgs> persistOutputCalledWith = new ArrayList<>();
    private List<AddNewJobStatusArgs> addNewJobStatusArgsCalledWith = new ArrayList<>();


    @Override
    public Disposable appendStdout(JobId jobId, Observable<byte[]> stdout) {
        persistStdoutCalledWith = Optional.of(jobId);
        return createMockDisposable(persistStdoutDisposed);
    }

    private Disposable createMockDisposable(AtomicBoolean b) {
        return  new Disposable() {
            @Override
            public void dispose() {
                b.set(true);
            }

            @Override
            public boolean isDisposed() {
                return b.get();
            }
        };
    }

    @Override
    public Disposable appendStderr(JobId jobId, Observable<byte[]> stderr) {
        persistStderrCalledWith = Optional.of(jobId);
        return createMockDisposable(persistStderrDisposed);
    }

    @Override
    public PersistedJob persist(ValidJobRequest validJobRequest) {
        persistCalledWith = Optional.of(validJobRequest);
        returnedPersistedReq = new PersistedJob(
                generateJobId(),
                validJobRequest.getOwner(),
                validJobRequest.getName(),
                validJobRequest.getInputs(),
                singletonList(generateJobStatusChangeTimestamp()),
                validJobRequest.getSpec());
        return returnedPersistedReq;
    }

    @Override
    public void addNewJobStatus(JobId jobId, JobStatus newStatus, String statusMessage) {
        addNewJobStatusArgsCalledWith.add(new AddNewJobStatusArgs(jobId, newStatus, statusMessage));
    }

    @Override
    public void persistOutput(JobId jobId, JobOutput jobOutput) {
        persistOutputCalledWith.add(new PersistOutputArgs(jobId, jobOutput.getId(), jobOutput.getData()));
    }

    @Override
    public void remove(JobId jobId) {

    }


    public Optional<JobId> getPersistStdoutCalledWith() {
        return persistStdoutCalledWith;
    }

    public AtomicBoolean getPersistStdoutDisposed() {
        return persistStdoutDisposed;
    }

    public Optional<JobId> getPersistStderrCalledWith() {
        return persistStderrCalledWith;
    }

    public AtomicBoolean getPersistStderrDisposed() {
        return persistStderrDisposed;
    }

    public Optional<ValidJobRequest> getPersistCalledWith() {
        return persistCalledWith;
    }

    public PersistedJob getReturnedPersistedReq() {
        return returnedPersistedReq;
    }

    public List<PersistOutputArgs> getPersistOutputCalledWith() {
        return persistOutputCalledWith;
    }

    public List<AddNewJobStatusArgs> getAddNewJobStatusArgsCalledWith() {
        return addNewJobStatusArgsCalledWith;
    }
}
