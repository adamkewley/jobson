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
package com.github.jobson.execution.finalizing;

import com.github.jobson.api.persistence.JobDetails;
import com.github.jobson.api.specs.JobSpec;
import org.junit.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public final class LocalJobFinalizerTest {

    @Test
    public void testFinalizeRemovesJobWorkingDirectoryIfRemoveWorkingDirectoriesIsEnabled() {
        final JobFinalizerConfig config = new JobFinalizerConfig(true);
        final JobFinalizerIO io = new NullJobFinalizerIO();
        final LocalJobFinalizer jobFinalizer = new LocalJobFinalizer(config, io);

        final JobDetails jobDetails;
        final JobSpec jobSpec;
        final Path workingDir;
        final int exitCode;
        final ExecutedJob executedJob = null;

        jobFinalizer.finalizeJob(executedJob);

        // TODO: Check the wd is deleted
    }

    @Test
    public void testFinalizeCopiesOutputsWithAbsolutePathsCorrectly() {
        assertThat(false).isTrue();
    }

    @Test
    public void testFinalizeDoesntThrowIfUsingAnAbsoluteExpectedOutputThatDoesntExist() {
        assertThat(false).isTrue();
    }

    @Test
    public void testFinalizeDoesntThrowIfUsingAnAbsoluteExpectedOutputThatDoesntExistANDTheWorkingDirectoryIsRelative() {
        assertThat(false).isTrue();
    }

    @Test
    public void testFinalizeSetsJobFinalStatusToSuccessIfEverythingOk() {
        assertThat(false).isTrue();
    }

    @Test
    public void testFinalizeSetsJobFinalStatusToErrorIfExitCodeNonzero() {
        assertThat(false).isTrue();
    }

    @Test
    public void testFinalizeSetsJobFinalStatusToErrorIfOutputMissing() {
        assertThat(false).isTrue();
    }

    @Test
    public void testFinalizeSetsJobFinalStatusToAbortedIfExitCodeIsAborted() {
        assertThat(false).isTrue();
    }

    @Test
    public void testFinalizePersistsExpectedOutputsToIOLayer() {
        assertThat(false).isTrue();
    }
}
