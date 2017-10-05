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

public final class SubprocessJobExecutorTest {

    /** TODO: Implement

    @Test
    public void testTrySubmitJobDirectoryWorkingDirectoryContainsDependenciesSpecifiedInJobSchema() throws IOException {
        final Path jobsDir = createTemporaryDirectory();
        final FilesystemJobsDAO dao = new FilesystemJobsDAO(jobsDir);

        final ValidJobRequest resolvedJobSubmissionRequest = resolvedSubmissionReqFixture(
                "fixtures/dao/FilesystemJobsDAO/resolved-request-with-dependencies.json");

        final JobId jobId = dao.persist(resolvedJobSubmissionRequest).get();

        final Path jobWorkingDirectory = jobsDir.resolve(jobId.toString()).resolve(Constants.WORKING_DIR_NAME);

        final Path firstDependency = jobWorkingDirectory.resolve("first");
        assertThat(Files.exists(firstDependency)).isTrue();

        final Path secondDependency = jobWorkingDirectory.resolve("second");
        assertThat(Files.exists(secondDependency)).isTrue();
    }

    **/

}