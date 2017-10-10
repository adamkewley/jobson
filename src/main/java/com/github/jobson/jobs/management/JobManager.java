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

import com.github.jobson.api.v1.JobId;
import com.github.jobson.api.v1.JobStatus;
import com.github.jobson.dao.jobs.WritingJobDAO;
import com.github.jobson.jobs.execution.JobExecutionResult;
import com.github.jobson.jobs.execution.JobExecutor;
import com.github.jobson.jobs.states.*;
import com.github.jobson.utils.CancelablePromise;
import com.github.jobson.utils.SimpleCancelablePromise;
import com.github.jobson.websockets.v1.JobEvent;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.github.jobson.Helpers.now;
import static com.github.jobson.Helpers.tryGet;
import static com.github.jobson.api.v1.JobStatus.*;
import static com.github.jobson.jobs.management.JobEventListeners.createNullListeners;

public final class JobManager implements JobManagerEvents, JobManagerActions {

    private final ConcurrentLinkedQueue<QueuedJob> jobQueue = new ConcurrentLinkedQueue<>();
    private final Map<JobId, ExecutingJob> executingJobs = Collections.synchronizedMap(new HashMap<>());
    private final Subject<JobEvent> jobEvents = PublishSubject.create();
    private final WritingJobDAO jobDAO;
    private final JobExecutor jobExecutor;
    private final int maxRunningJobs;


    public JobManager(WritingJobDAO jobDAO, JobExecutor jobExecutor, int maxRunningJobs) {
        this.jobDAO = jobDAO;
        this.jobExecutor = jobExecutor;
        this.maxRunningJobs = maxRunningJobs;
    }


    public Observable<JobEvent> allJobStatusChanges() {
        return jobEvents;
    }

    public Optional<Observable<byte[]>> stderrUpdates(JobId jobId) {
        return tryGet(executingJobs, jobId).map(ExecutingJob::getStderr);
    }

    public Optional<Observable<byte[]>> stdoutUpdates(JobId jobId) {
        return tryGet(executingJobs, jobId).map(ExecutingJob::getStdout);
    }

    public boolean tryAbort(JobId jobId) {
        return tryGet(executingJobs, jobId)
                .map(this::tryCancel)
                .orElseGet(() -> tryRemoveFromQueue(jobId));
    }

    private boolean tryCancel(ExecutingJob executingJob) {
        final boolean cancelled =
                executingJob.getCompletionPromise().cancel(true);

        if (cancelled) {
            updateJobStatus(executingJob.getId(), ABORTED, "Aborted");
        }

        return cancelled;
    }

    private void updateJobStatus(JobId jobId, JobStatus jobStatus, String message) {
        jobDAO.addNewJobStatus(jobId, jobStatus, message);
        jobEvents.onNext(new JobEvent(jobId, jobStatus));
    }

    private boolean tryRemoveFromQueue(JobId jobId) {
        return jobQueue.stream()
                .filter(queuedJob -> queuedJob.getId().equals(jobId))
                .findFirst()
                .map(this::tryRemove)
                .orElse(false);
    }

    private boolean tryRemove(QueuedJob queuedJob) {
        final boolean removed = jobQueue.remove(queuedJob);

        if (removed) {
            updateJobStatus(queuedJob.getId(), ABORTED, "Aborted");
        }

        return removed;
    }

    public Pair<JobId, CancelablePromise<FinalizedJob>> submit(ValidJobRequest validJobRequest) {
        return submit(validJobRequest, createNullListeners());
    }

    public Pair<JobId, CancelablePromise<FinalizedJob>> submit(ValidJobRequest validJobRequest, JobEventListeners listeners) {
        final PersistedJobRequest persistedJobRequest = jobDAO.persist(validJobRequest);
        final SimpleCancelablePromise<FinalizedJob> ret = new SimpleCancelablePromise<>();

        final QueuedJob queuedJob = QueuedJob.fromPersistedJobRequest(persistedJobRequest, listeners, ret);

        jobQueue.add(queuedJob);

        ret.onCancel(() -> tryAbort(persistedJobRequest.getId()));

        updateJobStatus(persistedJobRequest.getId(), SUBMITTED, "Queued by job manager");

        tryAdvancingJobQueue();

        return Pair.of(persistedJobRequest.getId(), ret);
    }

    private void tryAdvancingJobQueue() {
        if (executingJobs.size() >= maxRunningJobs) return;

        final QueuedJob queuedJob = jobQueue.poll();

        if (queuedJob == null) return;

        final Subject<byte[]> stdout = PublishSubject.create();
        final Subject<byte[]> stderr = PublishSubject.create();

        jobDAO.appendStdout(queuedJob.getId(), stdout);
        jobDAO.appendStderr(queuedJob.getId(), stderr);

        stdout.subscribe(queuedJob.getQueuedListeners().getOnStdoutListener());
        stderr.subscribe(queuedJob.getQueuedListeners().getOnStderrListener());

        final CancelablePromise<JobExecutionResult> executionPromise =
                jobExecutor.execute(queuedJob, JobEventListeners.create(stdout, stderr));

        final ExecutingJob executingJob =
                ExecutingJob.fromQueuedJob(queuedJob, now(), stdout, stderr);

        executingJobs.put(executingJob.getId(), executingJob);

        updateJobStatus(queuedJob.getId(), RUNNING, "Submitted to executor");

        executionPromise.thenAccept(res -> {
            onExecutionFinished(executingJob, res);
        });
    }

    private void onExecutionFinished(ExecutingJob executingJob, JobExecutionResult jobExecutionResult) {
        executingJobs.remove(executingJob.getId());

        jobExecutionResult.getOutputs().forEach((outputId, data) -> {
            final String resolvedMimeType = executingJob.getSpec().getOutputs().get(outputId).getMimeType();

            jobDAO.persistOutput(executingJob.getId(), outputId, data.withMimeType(resolvedMimeType));
        });

        updateJobStatus(executingJob.getId(), jobExecutionResult.getFinalStatus(), "Execution finished");

        final FinalizedJob finalizedJob =
                FinalizedJob.fromExecutingJob(executingJob, jobExecutionResult.getFinalStatus());

        executingJob.getCompletionPromise().complete(finalizedJob);
    }
}
