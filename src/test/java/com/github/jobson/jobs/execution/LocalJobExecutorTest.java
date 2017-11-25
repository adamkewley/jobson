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

import com.github.jobson.jobs.JobExecutor;
import com.github.jobson.jobs.LocalJobExecutor;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;

import static com.github.jobson.Constants.DELAY_BEFORE_FORCIBLY_KILLING_JOBS_IN_MILLISECONDS;
import static com.github.jobson.TestHelpers.createTmpDir;
import static com.github.jobson.TestHelpers.generateAlphanumStr;

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

    /**
    @Test
    public void testQueryVariableApplicationArgumentIsResolvedToQueryPath() throws IOException, InterruptedException {
        final Path jobsDir = createTemporaryDirectory();
        final FilesystemJobsDAO dao = createStandardFilesystemBasedJobsDAO(jobsDir);

        final ValidJobRequest validJobRequest = readResolvedSubmissionReq(
                "fixtures/dao/FilesystemBasedJobsDAO/request-with-query-template-variable.json");

        final JobId jobId = dao.persist(validJobRequest).get();

        // Give the child process a chance to init, echo to stderr, then exit
        Thread.sleep(PROCESS_WAIT_TIME_IN_MILLISECONDS);

        final Path stdout = dao.getStdout(jobId)
                .map(TestHelpers::toUtf8String)
                .map(String::trim)
                .map(s -> Paths.get(s))
                .get();

        assertThat(Files.exists(stdout));
        assertThat(readJSON(stdout, Object.class));
    }
    **/
}