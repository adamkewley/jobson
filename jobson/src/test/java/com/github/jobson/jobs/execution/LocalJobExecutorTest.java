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
import com.github.jobson.specs.RawTemplateString;
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

    @Override
    protected JobExecutor getInstance(Path workingDir) {
        try {
            return new LocalJobExecutor(workingDir.relativize(createTmpDir(LocalJobExecutorTest.class)), DELAY_BEFORE_FORCIBLY_KILLING_JOBS_IN_MILLISECONDS);
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
    public void testWdRemovalConfigEnabledCausesWorkingDirectoriesToBeRemovedAfterTheJobCompletes() throws IOException, InterruptedException, TimeoutException {
        // FIXME: This test is a bit of a hack to get around the job pipeline not being cleanly architected
        // it lets the job run + finish, but then needs to wait a while (1 sec) for the wd cleanup to happen
        final Path workingDir = Files.createTempDirectory(LocalJobExecutorTest.class.getSimpleName());

        final RemoveAfterExecutionConfig config = new RemoveAfterExecutionConfig(true);

        final LocalJobExecutor jobExecutor =
                new LocalJobExecutor(workingDir, DELAY_BEFORE_FORCIBLY_KILLING_JOBS_IN_MILLISECONDS, config);

        final PersistedJob req = createStandardRequest();

        // It's on a bg thread so that the job finalization can also get a chance to kick off
        final Thread t = new Thread(() -> {
            final CancelablePromise<JobExecutionResult> p = jobExecutor.execute(req, JobEventListeners.createNullListeners());

            try {
                p.get();
            } catch (Exception ex) {}
        });

        t.start();
        t.join();

        Thread.sleep(1000);

        assertThat(workingDir.resolve(req.getId().toString()).toFile().exists()).isFalse();
    }

    @Test
    public void testTemplatedDependencySourceIsResolvedAsATemplateString() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final String templatedSource = "${request.id}";
        final Path actualSourceDir = Files.createTempDirectory(JobExecutorTest.class.getSimpleName());
        final String templatedSourcePath = actualSourceDir.resolve(templatedSource).toString();

        final Path jobsDir = Files.createTempDirectory(LocalJobExecutorTest.class.getSimpleName());
        final String destinationName = generateAlphanumStr();
        final JobDependencyConfiguration dep = new JobDependencyConfiguration(templatedSourcePath, destinationName);
        final PersistedJob job = createStandardRequestWithDependency(dep);

        final Path pathToSourceFileContainingJobId = Files.createFile(actualSourceDir.resolve(job.getId().toString()));  // The job ID is in the template,
        final byte[] bytesInSourceFileNamedByJobId = TestHelpers.generateRandomBytes();
        Files.write(pathToSourceFileContainingJobId, bytesInSourceFileNamedByJobId);

        final LocalJobExecutor jobExecutor = new LocalJobExecutor(jobsDir, DELAY_BEFORE_FORCIBLY_KILLING_JOBS_IN_MILLISECONDS);

        final CancelablePromise<JobExecutionResult> p =
                jobExecutor.execute(job, JobEventListeners.createNullListeners());

        p.get();

        final Path destinationPath = jobsDir.resolve(job.getId().toString()).resolve(destinationName);

        final byte[] bytesInOutputFile = Files.readAllBytes(destinationPath);

        assertThat(Files.exists(destinationPath)).isTrue();
        assertThat(bytesInOutputFile).isEqualTo(bytesInSourceFileNamedByJobId);
    }

    @Test
    public void testTemplatedDependencyDestinationIsResolvedAsATemplateArg() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final Path sourceDir = Files.createTempDirectory(JobExecutorTest.class.getSimpleName());
        final Path sourceFile = Files.createFile(sourceDir.resolve(generateAlphanumStr()));
        final byte[] sourceBytes = TestHelpers.generateRandomBytes();
        Files.write(sourceFile, sourceBytes);

        final String templatedDestinationName = "${request.id}";
        final JobDependencyConfiguration dep = new JobDependencyConfiguration(sourceFile.toString(), templatedDestinationName);

        final PersistedJob job = createStandardRequestWithDependency(dep);
        final Path workingDir = Files.createTempDirectory(LocalJobExecutorTest.class.getSimpleName());
        final LocalJobExecutor jobExecutor = new LocalJobExecutor(workingDir, DELAY_BEFORE_FORCIBLY_KILLING_JOBS_IN_MILLISECONDS);

        final CancelablePromise<JobExecutionResult> p =
                jobExecutor.execute(job, JobEventListeners.createNullListeners());

        p.get();

        final Path expectedDestination = workingDir.resolve(job.getId().toString()).resolve(job.getId().toString());

        assertThat(expectedDestination.toFile().exists()).isTrue();

        final byte[] bytesInDestination = Files.readAllBytes(expectedDestination);

        assertThat(bytesInDestination).isEqualTo(sourceBytes);
    }

    @Test
    public void testTemplatedDependencySourceCanBeResolvedWithJobInputs() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final String templatedSource = "${inputs.foo}";  // In fixture: resolves to 'a'
        final Path actualSourceDir = Files.createTempDirectory(JobExecutorTest.class.getSimpleName());
        final String templatedSourcePath = actualSourceDir.resolve(templatedSource).toString();

        final Path workingDir = Files.createTempDirectory(LocalJobExecutorTest.class.getSimpleName());
        final Path destination = workingDir.resolve(generateAlphanumStr());
        final JobDependencyConfiguration dep = new JobDependencyConfiguration(templatedSourcePath, destination.toString());
        final PersistedJob job = createStandardRequestWithDependency(dep);

        final Path pathToSourceFileNamedByInput = Files.createFile(actualSourceDir.resolve("a"));  // "a" comes from the fixture
        final byte[] bytesInSourceFileNamedByJobId = TestHelpers.generateRandomBytes();
        Files.write(pathToSourceFileNamedByInput, bytesInSourceFileNamedByJobId);

        final LocalJobExecutor jobExecutor = new LocalJobExecutor(workingDir, DELAY_BEFORE_FORCIBLY_KILLING_JOBS_IN_MILLISECONDS);

        final CancelablePromise<JobExecutionResult> p =
                jobExecutor.execute(job, JobEventListeners.createNullListeners());

        p.get();

        final byte[] bytesInOutputFile = Files.readAllBytes(destination);

        assertThat(Files.exists(destination)).isTrue();
        assertThat(bytesInOutputFile).isEqualTo(bytesInSourceFileNamedByJobId);
    }

    @Test
    public void testTemplatedDependencyDestinationCanBeResolvedWithInputs() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        final Path sourceDir = Files.createTempDirectory(JobExecutorTest.class.getSimpleName());
        final Path sourceFile = Files.createFile(sourceDir.resolve(generateAlphanumStr()));
        final byte[] sourceBytes = TestHelpers.generateRandomBytes();
        Files.write(sourceFile, sourceBytes);

        final String templatedDestinationName = "${inputs.foo}";  // This is set in fixture
        final JobDependencyConfiguration dep = new JobDependencyConfiguration(sourceFile.toString(), templatedDestinationName);

        final PersistedJob job = createStandardRequestWithDependency(dep);
        final Path jobsDir = Files.createTempDirectory(LocalJobExecutorTest.class.getSimpleName());
        final LocalJobExecutor jobExecutor = new LocalJobExecutor(jobsDir, DELAY_BEFORE_FORCIBLY_KILLING_JOBS_IN_MILLISECONDS);

        final CancelablePromise<JobExecutionResult> p =
                jobExecutor.execute(job, JobEventListeners.createNullListeners());

        p.get();

        final Path expectedDestination = jobsDir.resolve(job.getId().toString()).resolve("a");  // "a" comes from fixture

        assertThat(expectedDestination.toFile().exists()).isTrue();

        final byte[] bytesInDestination = Files.readAllBytes(expectedDestination);

        assertThat(bytesInDestination).isEqualTo(sourceBytes);
    }
}