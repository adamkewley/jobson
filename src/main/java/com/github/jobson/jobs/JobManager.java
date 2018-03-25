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

package com.github.jobson.jobs;

import com.codahale.metrics.health.HealthCheck;
import com.github.jobson.dao.jobs.WritingJobDAO;
import com.github.jobson.jobs.jobstates.*;
import com.github.jobson.utils.CancelablePromise;
import com.github.jobson.utils.SimpleCancelablePromise;
import com.github.jobson.websockets.v1.JobEvent;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static com.github.jobson.Constants.JOB_MANAGER_JOB_QUEUE_OVERFLOW_HEALTHCHECK;
import static com.github.jobson.Constants.JOB_MANAGER_MAX_JOB_QUEUE_OVERFLOW_THRESHOLD;
import static com.github.jobson.Helpers.now;
import static com.github.jobson.Helpers.tryGet;
import static com.github.jobson.jobs.JobStatus.*;
import static java.lang.String.format;

public final class JobManager implements JobManagerEvents, JobManagerActions {

    private static Logger log = LoggerFactory.getLogger(JobManager.class);

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
        log.debug("Received cancellation signal for executing job " + executingJob.getId());

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
        return submit(validJobRequest, JobEventListeners.createNullListeners());
    }

    public Pair<JobId, CancelablePromise<FinalizedJob>> submit(ValidJobRequest validJobRequest, JobEventListeners listeners) {
        final PersistedJob persistedJob = jobDAO.persist(validJobRequest);
        final SimpleCancelablePromise<FinalizedJob> ret = new SimpleCancelablePromise<>();

        final QueuedJob queuedJob = QueuedJob.fromPersistedJobRequest(persistedJob, listeners, ret);

        jobQueue.add(queuedJob);

        ret.onCancel(() -> tryAbort(persistedJob.getId()));

        updateJobStatus(persistedJob.getId(), SUBMITTED, "Queued by job manager");

        tryAdvancingJobQueue();

        return Pair.of(persistedJob.getId(), ret);
    }

    private void tryAdvancingJobQueue() {
        while (jobQueue.size() > 0 && executingJobs.size() < maxRunningJobs)
            advanceJobQueue();
    }

    private void advanceJobQueue() {
        final QueuedJob queuedJob = jobQueue.poll();

        if (queuedJob == null) return;

        final Subject<byte[]> stdout = PublishSubject.create();
        final Subject<byte[]> stderr = PublishSubject.create();

        jobDAO.appendStdout(queuedJob.getId(), stdout);
        jobDAO.appendStderr(queuedJob.getId(), stderr);

        stdout.subscribe(queuedJob.getQueuedListeners().getOnStdoutListener());
        stderr.subscribe(queuedJob.getQueuedListeners().getOnStderrListener());

        try {
            final CancelablePromise<JobExecutionResult> executionPromise =
                    jobExecutor.execute(queuedJob, JobEventListeners.create(stdout, stderr));

            final ExecutingJob executingJob =
                    ExecutingJob.fromQueuedJob(queuedJob, now(), stdout, stderr);

            executingJobs.put(executingJob.getId(), executingJob);

            updateJobStatus(queuedJob.getId(), RUNNING, "Submitted to executor");

            executionPromise.thenAccept(res -> {
                onExecutionFinished(executingJob, res);
            });

            executingJob.getCompletionPromise().onCancel(() -> {
                executionPromise.cancel(true);
            });
        } catch (Throwable ex) {
            log.error("Error starting job execution: " + ex.toString());
            updateJobStatus(queuedJob.getId(), FATAL_ERROR, "Error executing job: " + ex.toString());
        }
    }

    private void onExecutionFinished(ExecutingJob executingJob, JobExecutionResult jobExecutionResult) {
        executingJobs.remove(executingJob.getId());

        final FinalizedJob finalizedJob = finalizeJob(executingJob, jobExecutionResult);

        updateJobStatus(finalizedJob.getId(), finalizedJob.getFinalStatus(), finalizedJob.getFinalMessage());
        executingJob.getCompletionPromise().complete(finalizedJob);
        tryAdvancingJobQueue();
    }

    private FinalizedJob finalizeJob(ExecutingJob executingJob, JobExecutionResult jobExecutionResult) {
        final JobStatus statusFromExecutor = jobExecutionResult.getFinalStatus();

        final JobStatus finalStatus;
        final String finalMessage;
        if (statusFromExecutor.equals(JobStatus.FINISHED)) {
            final Optional<String> outputHandlingError =
                    handleOutputPersistence(executingJob, jobExecutionResult);

            if (outputHandlingError.isPresent()) {
                finalStatus = JobStatus.FATAL_ERROR;
                finalMessage = "Job executed successfully, but there was an error handling the outputs: " + outputHandlingError.get();
            } else {
                finalStatus = statusFromExecutor;
                finalMessage = "Execution finished";
            }
        } else {
            finalStatus = statusFromExecutor;
            finalMessage = "Execution did not finish successfully";
        }

        return FinalizedJob.fromExecutingJob(executingJob, finalStatus, finalMessage);
    }

    private Optional<String> handleOutputPersistence(ExecutingJob executingJob, JobExecutionResult jobExecutionResult) {
        final JobOutputPersister outputPersister = new JobOutputPersister(executingJob.getId(), jobDAO);

        final String errors = jobExecutionResult.getOutputs()
                .stream()
                .map(outputResult -> outputResult.accept(outputPersister))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining(", "));

        return errors.isEmpty() ? Optional.empty() : Optional.of(errors);
    }

    public Map<String, HealthCheck> getHealthChecks() {
        return Collections.singletonMap(
                JOB_MANAGER_JOB_QUEUE_OVERFLOW_HEALTHCHECK,
                new HealthCheck() {
                    @Override
                    protected Result check() throws Exception {
                        final int queueSize = jobQueue.size();
                        if (queueSize < JOB_MANAGER_MAX_JOB_QUEUE_OVERFLOW_THRESHOLD) {
                            return Result.healthy(format("Queue contains %s entries", queueSize));
                        } else {
                            return Result.unhealthy(format(
                                    "%s entries in job queue: this exceeds the warning threshold (%s)",
                                    queueSize,
                                    JOB_MANAGER_MAX_JOB_QUEUE_OVERFLOW_THRESHOLD));
                        }
                    }
                });
    }
}
