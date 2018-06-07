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

package com.github.jobson.jobs.execution;

import com.github.jobson.TestHelpers;
import com.github.jobson.config.RemoveAfterExecutionConfig;
import com.github.jobson.jobs.JobEventListeners;
import com.github.jobson.jobs.JobExecutionResult;
import com.github.jobson.jobs.JobExecutor;
import com.github.jobson.jobs.LocalJobExecutor;
import com.github.jobson.jobs.jobstates.PersistedJob;
import com.github.jobson.specs.JobDependencyConfiguration;
import com.github.jobson.utils.CancelablePromise;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.github.jobson.Constants.DELAY_BEFORE_FORCIBLY_KILLING_JOBS_IN_MILLISECONDS;
import static com.github.jobson.TestHelpers.createTmpDir;
import static com.github.jobson.TestHelpers.generateAlphanumStr;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public final class LocalJobExecutorTest extends JobExecutorTest {

    @Override
    protected JobExecutor getInstance() {
        try {
            return new LocalJobExecutor(createTmpDir(LocalJobExecutorTest.class), DELAY_BEFORE_FORCIBLY_KILLING_JOBS_IN_MILLISECONDS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Test(expected = NullPointerException.class)
    public void testCtorThrowsIfWorkingDirsIsNull() throws FileNotFoundException {
        new LocalJobExecutor(null, DELAY_BEFORE_FORCIBLY_KILLING_JOBS_IN_MILLISECONDS);
    }

    @Test(expected = FileNotFoundException.class)
    public void testCtorThrowsIfWorkingDirsDoesNotExist() throws FileNotFoundException {
        new LocalJobExecutor(Paths.get(generateAlphanumStr()), DELAY_BEFORE_FORCIBLY_KILLING_JOBS_IN_MILLISECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCtorThrowsIfDelayIsNegative() throws IOException {
        new LocalJobExecutor(createTmpDir(LocalJobExecutorTest.class), -1);
    }

    @Test
    public void testFileDependencyIsCopiedWithExecutePermissionsMaintained() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final File someFile = File.createTempFile("dependencytest", "");
        assertThat(someFile.canExecute()).isFalse();
        someFile.setExecutable(true);
        final Path source = someFile.toPath().toAbsolutePath();

        final Path workingDir = createTmpDir(LocalJobExecutor.class);
        final Path dest = workingDir.resolve("copied-file");

        final JobDependencyConfiguration dep = new JobDependencyConfiguration(source.toString(), dest.toString());
        final PersistedJob job = createStandardRequestWithDependency(dep);

        final LocalJobExecutor jobExecutor = new LocalJobExecutor(workingDir, DELAY_BEFORE_FORCIBLY_KILLING_JOBS_IN_MILLISECONDS);

        final CancelablePromise<JobExecutionResult> p =
                jobExecutor.execute(job, JobEventListeners.createNullListeners());

        p.get();

        final File destAfterCopy = new File(dest.toString());

        assertThat(destAfterCopy.canExecute()).isTrue();
    }

    @Test
    public void testDirectoryDependencyIsCopiedWithFileExecutePermissionMaintained() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final Path sourceDir = Files.createTempDirectory(JobExecutorTest.class.getSimpleName());
        final String filenameOfExecutableFileInSrcDir = "some-file-with-exec-permissions";
        final File fileInSourceDir = Files.createFile(sourceDir.resolve(filenameOfExecutableFileInSrcDir)).toFile();
        fileInSourceDir.setExecutable(true);

        final Path workingDir = createTmpDir(LocalJobExecutor.class);
        final Path dest = workingDir.resolve("copied-dir");

        final JobDependencyConfiguration dep = new JobDependencyConfiguration(
                sourceDir.toAbsolutePath().toString(),
                dest.toAbsolutePath().toString());
        final PersistedJob job = createStandardRequestWithDependency(dep);

        final LocalJobExecutor jobExecutor = new LocalJobExecutor(workingDir, DELAY_BEFORE_FORCIBLY_KILLING_JOBS_IN_MILLISECONDS);

        final CancelablePromise<JobExecutionResult> p =
                jobExecutor.execute(job, JobEventListeners.createNullListeners());

        p.get();

        final File fileAfterCopy = new File(dest.resolve(filenameOfExecutableFileInSrcDir).toString());

        assertThat(fileAfterCopy.canExecute()).isTrue();
    }

    @Test
    public void testSoftlinkedFileDependencyIsSoftLinkedFromTheDestinationToTheSource() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final Path sourceDir = Files.createTempDirectory(JobExecutorTest.class.getSimpleName());
        final Path sourceFile = Files.createFile(sourceDir.resolve(generateAlphanumStr()));

        final Path workingDir = Files.createTempDirectory(LocalJobExecutorTest.class.getSimpleName());
        final Path destination = workingDir.resolve(generateAlphanumStr());

        final JobDependencyConfiguration dep = new JobDependencyConfiguration(
                sourceFile.toString(),
                destination.toString(),
                true);

        final PersistedJob job = createStandardRequestWithDependency(dep);

        final LocalJobExecutor jobExecutor = new LocalJobExecutor(workingDir, DELAY_BEFORE_FORCIBLY_KILLING_JOBS_IN_MILLISECONDS);

        final CancelablePromise<JobExecutionResult> p =
                jobExecutor.execute(job, JobEventListeners.createNullListeners());

        p.get();

        assertThat(Files.isSymbolicLink(destination)).isTrue();
        assertThat(Files.readSymbolicLink(destination)).isEqualTo(sourceFile);
    }

    @Test
    public void testWdRemovalConfigEnabledCausesWorkingDirectoriesToBeRemovedAfterTheJobCompletes() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final Path workingDir = Files.createTempDirectory(LocalJobExecutorTest.class.getSimpleName());

        final RemoveAfterExecutionConfig config = new RemoveAfterExecutionConfig(true);

        final LocalJobExecutor jobExecutor =
                new LocalJobExecutor(workingDir, DELAY_BEFORE_FORCIBLY_KILLING_JOBS_IN_MILLISECONDS, config);

        final PersistedJob req = createStandardRequest();

        final CancelablePromise<JobExecutionResult> p = jobExecutor.execute(req, JobEventListeners.createNullListeners());

        p.get();

        assertThat(workingDir.resolve(req.getId().toString()).toFile().exists()).isFalse();
    }
}