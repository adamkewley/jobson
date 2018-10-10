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
package com.github.jobson.execution;

import com.github.jobson.api.specs.JobOutputId;
import com.github.jobson.api.specs.JobSpecId;
import com.github.jobson.persistence.jobs.JobId;
import com.github.jobson.persistence.jobs.PersistedJob;

import java.nio.file.Path;
import java.util.Optional;

public interface NewJobExecutorIO {

    Optional<PersistedJob> getJobDetails(JobId jobId);

    void setJobAsExecuting(JobId jobId);

    void copyFileRelativeToSpecDir(JobSpecId specId, Path source, Path target);
    void softlinkFileRelativeToSpecDir(JobSpecId specId, Path source, Path linkName);

    void resetOutput(JobId jobId, JobOutputId outputId);
    void appendDataToOutput(JobId jobId, JobOutputId outputId, byte[] bytes);
    void copyFileAsOutput(JobId jobId, JobOutputId outputId, Path sourceFile);
    void moveFileAsOutput(JobId jobId, JobOutputId outputId, Path sourceFile);
    void writeOutputMetadata(JobId jobId, JobOutputId outputId, JobOutputMetadata jobOutputMetadata);
    void lockOutput(JobId jobId, JobOutputId jobOutputId);

    void finalizeJobAsSuccess(JobId jobId);
    void finalizeJobAsFailure(JobId jobId, String reason);
    void finalizeJobAsAborted(JobId jobId);
}
