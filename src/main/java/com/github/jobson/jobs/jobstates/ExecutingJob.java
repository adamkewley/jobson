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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.jobson.api.v1.UserId;
import com.github.jobson.jobinputs.JobExpectedInputId;
import com.github.jobson.jobinputs.JobInput;
import com.github.jobson.jobs.JobId;
import com.github.jobson.jobs.JobTimestamp;
import com.github.jobson.specs.JobSpec;
import com.github.jobson.utils.CancelablePromise;
import io.reactivex.Observable;

import java.util.Date;
import java.util.List;
import java.util.Map;

public final class ExecutingJob extends PersistedJob {

    public static ExecutingJob fromQueuedJob(
            QueuedJob queuedJob,
            Date date,
            Observable<byte[]> stdoutObservable,
            Observable<byte[]> stderrObservable) {

        return new ExecutingJob(
                queuedJob.getId(),
                queuedJob.getOwner(),
                queuedJob.getName(),
                queuedJob.getInputs(),
                queuedJob.getTimestamps(),
                queuedJob.getSpec(),
                date,
                stdoutObservable,
                stderrObservable,
                queuedJob.getCompletionPromise());
    }


    private final Date executionStarted;

    @JsonIgnore
    private final Observable<byte[]> stdout;

    @JsonIgnore
    private final Observable<byte[]> stderr;

    @JsonIgnore
    private final CancelablePromise<FinalizedJob> completionPromise;


    public ExecutingJob(
            JobId id,
            UserId owner,
            String name,
            Map<JobExpectedInputId, JobInput> inputs,
            List<JobTimestamp> timestamps,
            JobSpec spec,
            Date executionStarted,
            Observable<byte[]> stdout,
            Observable<byte[]> stderr,
            CancelablePromise<FinalizedJob> completionPromise) {

        super(id, owner, name, inputs, timestamps, spec);

        this.executionStarted = executionStarted;
        this.stdout = stdout;
        this.stderr = stderr;
        this.completionPromise = completionPromise;
    }

    public Date getExecutionStarted() {
        return this.executionStarted;
    }

    public Observable<byte[]> getStdout() {
        return stdout;
    }

    public Observable<byte[]> getStderr() {
        return stderr;
    }

    public CancelablePromise<FinalizedJob> getCompletionPromise() {
        return completionPromise;
    }
}
