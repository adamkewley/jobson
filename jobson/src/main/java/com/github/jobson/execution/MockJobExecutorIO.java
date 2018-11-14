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

import com.github.jobson.api.persistence.JobId;
import com.github.jobson.api.specs.JobOutputId;
import com.github.jobson.api.specs.JobSpecId;
import com.github.jobson.internal.JobOutputMetadata;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Optional;

public final class MockJobExecutorIO implements JobExecutorIO {

    @Override
    public Optional<QueuedJob> getQueuedJobById(JobId jobId) {
        return Optional.empty();
    }

    @Override
    public void setJobAsExecuting(JobId jobId) {

    }

    @Override
    public void copyDependency(JobSpecId specId, Path source, Path target) {

    }

    @Override
    public void softlinkDependency(JobSpecId specId, Path linkTarget, Path linkName) {

    }

    @Override
    public void appendStdout(JobId jobId, ByteBuffer bytes) {

    }

    @Override
    public void appendStderr(JobId jobId, ByteBuffer bytes) {

    }

    @Override
    public void copyFileToOutput(JobId jobId, JobOutputId outputId, Path sourceFile) {

    }

    @Override
    public void moveFileToOutput(JobId jobId, JobOutputId outputId, Path sourceFile) {

    }

    @Override
    public void writeOutputMetadata(JobId jobId, JobOutputId outputId, JobOutputMetadata jobOutputMetadata) {

    }

    @Override
    public void finalizeJobAsSuccess(JobId jobId) {

    }

    @Override
    public void finalizeJobAsFailure(JobId jobId, String reason) {

    }

    @Override
    public void finalizeJobAsAborted(JobId jobId) {

    }
}
