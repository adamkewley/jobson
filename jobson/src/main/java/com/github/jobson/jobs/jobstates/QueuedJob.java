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
import com.github.jobson.jobs.JobEventListeners;
import com.github.jobson.jobs.JobId;
import com.github.jobson.jobs.JobTimestamp;
import com.github.jobson.specs.JobSpec;
import com.github.jobson.utils.CancelablePromise;
import com.github.jobson.utils.SimpleCancelablePromise;

import java.util.List;
import java.util.Map;

public final class QueuedJob extends PersistedJob {

    public static QueuedJob fromPersistedJobRequest(
            PersistedJob persistedJob,
            JobEventListeners listeners,
            SimpleCancelablePromise<FinalizedJob> promise) {

        return new QueuedJob(
                persistedJob.getId(),
                persistedJob.getOwner(),
                persistedJob.getName(),
                persistedJob.getInputs(),
                persistedJob.getTimestamps(),
                persistedJob.getSpec(),
                listeners,
                promise);
    }

    @JsonIgnore
    private final JobEventListeners queuedListeners;

    @JsonIgnore
    private final CancelablePromise<FinalizedJob> completionPromise;


    public QueuedJob(
            JobId id,
            UserId owner,
            String name,
            Map<JobExpectedInputId, JobInput> inputs,
            List<JobTimestamp> timestamps,
            JobSpec spec,
            JobEventListeners jobEventListeners,
            CancelablePromise<FinalizedJob> completionPromise) {

        super(id, owner, name, inputs, timestamps, spec);

        this.queuedListeners = jobEventListeners;
        this.completionPromise = completionPromise;
    }


    public JobEventListeners getQueuedListeners() {
        return queuedListeners;
    }

    public CancelablePromise<FinalizedJob> getCompletionPromise() {
        return completionPromise;
    }
}
