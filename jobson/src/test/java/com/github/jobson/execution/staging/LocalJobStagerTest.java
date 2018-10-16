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
package com.github.jobson.execution.staging;

import com.github.jobson.api.persistence.JobDetails;
import com.github.jobson.api.specs.JobSpec;
import com.github.jobson.api.specs.JobSpecId;
import com.github.jobson.execution.finalizing.LocalJobFinalizer;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public final class LocalJobStagerTest {

    private static LocalJobStager createInstance() throws IOException {
        final Path workingDirs = Files.createTempDirectory(LocalJobStager.class.getName());

        final JobStagerIO jobStagerIO = new JobStagerIO() {
            @Override
            public void copyDependency(JobSpecId specId, Path source, Path target) {
                throw new RuntimeException("NYI");
            }

            @Override
            public void softlinkDependency(JobSpecId specId, Path linkTarget, Path linkName) {
                throw new RuntimeException("NYI");
            }
        };

        return new LocalJobStager(workingDirs, jobStagerIO);
    }

    @Test
    public void testStageJobEvaluatesJobInputsAsExpected() throws IOException {
        final LocalJobStager jobStager = createInstance();
        final JobDetails jobDetails = null;  // TODO
        final JobSpec jobSpec = null;  //
    }

    // IMPORTED:

    @Test
    public void testExecuteEvaluatesJobInputsAsExpected() throws InterruptedException {
        final JobExecutor jobExecutor = getInstance();
        final PersistedJob req =
                standardRequestWithCommand("echo", "${inputs.foo}");
        final AtomicReference<byte[]> bytesEchoedToStdout = new AtomicReference<>(new byte[]{});
        final Subject<byte[]> stdoutSubject = PublishSubject.create();

        stdoutSubject.subscribe(bytes ->
                bytesEchoedToStdout.getAndUpdate(existingBytes ->
                        Bytes.concat(existingBytes, bytes)));

        final Semaphore s = new Semaphore(1);
        s.acquire();
        stdoutSubject.doOnComplete(s::release).subscribe();

        final JobEventListeners listeners =
                createStdoutListener(stdoutSubject);

        jobExecutor.execute(req, listeners);

        s.tryAcquire(TestConstants.DEFAULT_TIMEOUT, MILLISECONDS);

        final String stringFromStdout = new String(bytesEchoedToStdout.get()).trim();
        assertThat(stringFromStdout).isEqualTo("a"); // from spec
    }

    @Test
    public void testExecuteEvaluatesToJSONFunctionAsExpected() throws InterruptedException, IOException {
        final JobExecutor jobExecutor = getInstance();
        final PersistedJob req =
                standardRequestWithCommand("echo", "${toJSON(inputs)}");
        final AtomicReference<byte[]> bytesEchoedToStdout = new AtomicReference<>(new byte[]{});
        final Subject<byte[]> stdoutSubject = PublishSubject.create();

        stdoutSubject.subscribe(bytes ->
                bytesEchoedToStdout.getAndUpdate(existingBytes ->
                        Bytes.concat(existingBytes, bytes)));

        final Semaphore s = new Semaphore(1);
        s.acquire();
        stdoutSubject.doOnComplete(s::release).subscribe();

        final JobEventListeners listeners =
                createStdoutListener(stdoutSubject);

        jobExecutor.execute(req, listeners);

        s.tryAcquire(TestConstants.DEFAULT_TIMEOUT, MILLISECONDS);

        final String stringFromStdout = new String(bytesEchoedToStdout.get()).trim();

        TestHelpers.assertJSONEqual(stringFromStdout, toJSON(STANDARD_REQUEST.getInputs()));
    }

    @Test
    public void testExecuteEvaluatesToFileAsExpected() throws InterruptedException, IOException {
        final JobExecutor jobExecutor = getInstance();
        final PersistedJob req =
                standardRequestWithCommand("echo", "${toFile(toJSON(inputs))}");
        final AtomicReference<byte[]> bytesEchoedToStdout = new AtomicReference<>(new byte[]{});
        final Subject<byte[]> stdoutSubject = PublishSubject.create();

        stdoutSubject.subscribe(bytes ->
                bytesEchoedToStdout.getAndUpdate(existingBytes ->
                        Bytes.concat(existingBytes, bytes)));

        final Semaphore s = new Semaphore(1);
        s.acquire();
        stdoutSubject.doOnComplete(s::release).subscribe();

        final JobEventListeners listeners =
                createStdoutListener(stdoutSubject);

        jobExecutor.execute(req, listeners);

        s.tryAcquire(TestConstants.DEFAULT_TIMEOUT, MILLISECONDS);

        final String stringFromStdout = new String(bytesEchoedToStdout.get()).trim();
        final Path p = Paths.get(stringFromStdout);

        assertThat(p.toFile().exists());

        final String loadedJson = new String(Files.readAllBytes(p));

        TestHelpers.assertJSONEqual(loadedJson, toJSON(STANDARD_REQUEST.getInputs()));
    }

    @Test
    public void testExecuteEvaluatesJoinAsExpected() throws InterruptedException {
        final JobExecutor jobExecutor = getInstance();
        final PersistedJob req =
                standardRequestWithCommand("echo", "${join(',', inputs.someList)}");
        final AtomicReference<byte[]> bytesEchoedToStdout = new AtomicReference<>(new byte[]{});
        final Subject<byte[]> stdoutSubject = PublishSubject.create();
        stdoutSubject.subscribe(bytes ->
                bytesEchoedToStdout.getAndUpdate(existingBytes ->
                        Bytes.concat(existingBytes, bytes)));

        final Semaphore s = new Semaphore(1);
        s.acquire();
        stdoutSubject.doOnComplete(s::release).subscribe();

        final JobEventListeners listeners =
                createStdoutListener(stdoutSubject);

        jobExecutor.execute(req, listeners);

        s.tryAcquire(TestConstants.DEFAULT_TIMEOUT, MILLISECONDS);

        final String stringFromStdout = new String(bytesEchoedToStdout.get()).trim();

        assertThat(stringFromStdout).isEqualTo("a,b,c,d"); // From the input fixture
    }

    @Test
    public void testExecuteEvaluatesToStringAsExpected() throws InterruptedException {
        final JobExecutor jobExecutor = getInstance();
        final PersistedJob req =
                standardRequestWithCommand("echo", "${toString(inputs.someString)}");
        final AtomicReference<byte[]> bytesEchoedToStdout = new AtomicReference<>(new byte[]{});
        final Subject<byte[]> stdoutSubject = PublishSubject.create();
        stdoutSubject.subscribe(bytes ->
                bytesEchoedToStdout.getAndUpdate(existingBytes ->
                        Bytes.concat(existingBytes, bytes)));

        final Semaphore s = new Semaphore(1);
        s.acquire();
        stdoutSubject.doOnComplete(s::release).subscribe();

        final JobEventListeners listeners =
                createStdoutListener(stdoutSubject);

        jobExecutor.execute(req, listeners);

        s.tryAcquire(TestConstants.DEFAULT_TIMEOUT, MILLISECONDS);

        final String stringFromStdout = new String(bytesEchoedToStdout.get()).trim();

        assertThat(stringFromStdout).isEqualTo("hello, world!"); // from input fixture
    }


    @Test
    public void testExecuteEvaluatesOutputDirAsExpected() throws InterruptedException {
        final JobExecutor jobExecutor = getInstance();
        final PersistedJob req =
                standardRequestWithCommand("echo", "${outputDir}");
        final AtomicReference<byte[]> bytesEchoedToStdout = new AtomicReference<>(new byte[]{});
        final Subject<byte[]> stdoutSubject = PublishSubject.create();
        stdoutSubject.subscribe(bytes ->
                bytesEchoedToStdout.getAndUpdate(existingBytes ->
                        Bytes.concat(existingBytes, bytes)));

        final Semaphore s = new Semaphore(1);
        s.acquire();
        stdoutSubject.doOnComplete(s::release).subscribe();

        final JobEventListeners listeners =
                createStdoutListener(stdoutSubject);

        jobExecutor.execute(req, listeners);

        s.tryAcquire(TestConstants.DEFAULT_TIMEOUT, MILLISECONDS);

        final String stringFromStdout = new String(bytesEchoedToStdout.get()).trim();

        assertThat(Files.exists(Paths.get(stringFromStdout)));
    }

    @Test
    public void testExecuteEvaluatesTemplateStringsInTheExpectedOutputs() throws Throwable {
        final JobExecutor jobExecutor = getInstance();

        final RawTemplateString rawTemplateIdStr = new RawTemplateString("${request.id}");
        final RawTemplateString rawTemplatePathStr = new RawTemplateString("${toString('foo')}");
        final JobExpectedOutput jobExpectedOutput = new JobExpectedOutput(rawTemplateIdStr, rawTemplatePathStr, "application/octet-stream");
        final List<JobExpectedOutput> expectedOutputs = Collections.singletonList(jobExpectedOutput);

        final PersistedJob req =
                standardRequestWithExpectedOutputs(expectedOutputs, "touch", "foo");

        final JobEventListeners listeners = createNullListeners();

        promiseAssert(
                jobExecutor.execute(req, listeners),
                result -> {
                    assertThat(result.getOutputs()).isNotEmpty();

                    final JobOutputId expectedOutputIdAfterEvaluation =
                            new JobOutputId(STANDARD_REQUEST.getId().toString());

                    final Optional<JobOutput> maybeJobOutput =
                            getJobOutputById(result.getOutputs(), expectedOutputIdAfterEvaluation);

                    assertThat(maybeJobOutput).isPresent();

                    final JobOutput jobOutput = maybeJobOutput.get();

                    assertThat(jobOutput.getId().toString()).isEqualTo(req.getId().toString());
                });
    }

}