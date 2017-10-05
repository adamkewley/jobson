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

package com.github.jobson.dao.jobs;

import com.github.jobson.Constants;
import com.github.jobson.Helpers;
import com.github.jobson.TestHelpers;
import com.github.jobson.api.v1.JobDetailsResponse;
import com.github.jobson.dao.IdGenerator;
import com.github.jobson.jobs.states.PersistedJobRequest;
import com.github.jobson.jobs.states.ValidJobRequest;
import com.github.jobson.specs.JobSpec;
import com.github.jobson.api.v1.JobId;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.jobson.Helpers.readJSON;
import static com.github.jobson.Helpers.tryResolve;
import static org.assertj.core.api.Assertions.assertThat;

public final class FilesystemJobsDAOTest extends JobsDAOTest {

    private static Path createTmpDir() throws IOException {
        return Files.createTempDirectory(FilesystemJobsDAOTest.class.getSimpleName());
    }

    private static FilesystemJobsDAO createStandardFilesystemDAO() {
        try {
            final Path jobDir = createTmpDir();
            return createStandardFilesystemDAO(jobDir);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static FilesystemJobsDAO createStandardFilesystemDAO(Path jobDir) throws IOException {
        final IdGenerator idGenerator = createIdGenerator();

        return new FilesystemJobsDAO(jobDir, idGenerator);
    }

    private static IdGenerator createIdGenerator() {
        return () -> Helpers.generateRandomBase36String(10);
    }



    @Override
    protected JobDAO getInstance() {
        return createStandardFilesystemDAO();
    }



    @Test(expected = NullPointerException.class)
    public void testCtorThrowsIfPassedNulls() throws IOException {
        final Path jobDir = createTmpDir();
        final IdGenerator idGenerator = createIdGenerator();

        new FilesystemJobsDAO(jobDir, null);
        new FilesystemJobsDAO(null, idGenerator);
    }

    @Test(expected = FileNotFoundException.class)
    public void testCtorThrowsIfPassedANonExistentJobsDir() throws IOException {
        final Path invalidPath = Paths.get(TestHelpers.genrateRandomAlphanumericString());
        createStandardFilesystemDAO(invalidPath);
    }



    @Test
    public void testPersistNewJobCreatesAJobDirNamedWithTheJobID() throws IOException {
        final Path jobsDir = createTmpDir();
        final JobId jobId = persistValidRequest(jobsDir).getId();
        assertThat(Helpers.tryResolve(jobsDir, jobId)).isPresent();
    }

    private PersistedJobRequest persistValidRequest(Path jobsDir) throws IOException {
        return persistRequest(jobsDir, TestHelpers.STANDARD_VALID_REQUEST);
    }

    private PersistedJobRequest persistRequest(Path jobsDir, ValidJobRequest jobRequest) throws IOException {
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);
        return dao.persist(jobRequest);
    }

    @Test
    public void testPersistNewJobJobDirectoryContainsAJobRequestJSONFile() throws IOException {
        final Path jobsDir = createTmpDir();
        final JobId jobId = persistValidRequest(jobsDir).getId();
        Assertions.assertThat(Helpers.tryResolve(jobsDir, jobId, Constants.JOB_REQUEST_FILENAME)).isPresent();
    }

    @Test
    public void testPersistNewJobJobDirectoryJobRequestJSONFileIsValidJSON() throws IOException {
        final Path jobsDir = createTmpDir();
        final JobId jobId = persistValidRequest(jobsDir).getId();
        Helpers.readJSON(Helpers.tryResolve(jobsDir, jobId, Constants.JOB_REQUEST_FILENAME).get(), Object.class);
    }

    @Test
    public void testPersistNewJobJobDirectoryContainsAJobMetadataFile() throws IOException {
        final Path jobsDir = createTmpDir();
        final JobId jobId = persistValidRequest(jobsDir).getId();
        Assertions.assertThat(Helpers.tryResolve(jobsDir, jobId, Constants.METADATA_FILENAME)).isPresent();
    }

    @Test
    public void testPersistNewJobJobDirectoryJobMetadataFileIsValidJSON() throws IOException {
        final Path jobsDir = createTmpDir();
        final JobId jobId = persistValidRequest(jobsDir).getId();
        Helpers.readJSON(Helpers.tryResolve(jobsDir, jobId, Constants.METADATA_FILENAME).get(), Object.class);
    }

    @Test
    public void testPersistNewJobJobDirectoryJobMetadataFileParsesToAJobMetadataObject() throws IOException {
        final Path jobsDir = createTmpDir();
        final JobId jobId = persistValidRequest(jobsDir).getId();
        Helpers.readJSON(Helpers.tryResolve(jobsDir, jobId, Constants.METADATA_FILENAME).get(), JobDetailsResponse.class);
    }

    @Test
    public void testPersistNewJobJobDirectoryContainsTheJobsSchema() throws IOException {
        final Path jobsDir = createTmpDir();
        final JobId jobId = persistValidRequest(jobsDir).getId();
        Assertions.assertThat(Helpers.tryResolve(jobsDir, jobId, Constants.JOB_SCHEMA_FILENAME)).isPresent();
    }

    @Test
    public void testPersistNewJobJobDirectorySchemaFileIsValidJSON() throws IOException {
        final Path jobsDir = createTmpDir();
        final JobId jobId = persistValidRequest(jobsDir).getId();
        Assertions.assertThat(Helpers.tryResolve(jobsDir, jobId, Constants.JOB_SCHEMA_FILENAME)).isPresent();
    }


    @Test
    public void testPersistNewJobJobDirectoryJSONParsesToAJobSchemaConfiguration() throws IOException {
        final Path jobsDir = createTmpDir();
        final JobId jobId = persistValidRequest(jobsDir).getId();
        Helpers.readJSON(Helpers.tryResolve(jobsDir, jobId, Constants.JOB_SCHEMA_FILENAME).get(), JobSpec.class);
    }



    @Test
    public void testHasStdoutReturnsFalseIfTheStdoutFileWasDeleted() throws IOException {
        final Path jobsDir = createTmpDir();
        final FilesystemJobsDAO filesystemJobsDAO = createStandardFilesystemDAO(jobsDir);
        final JobId jobId = filesystemJobsDAO.persist(TestHelpers.STANDARD_VALID_REQUEST).getId();
        filesystemJobsDAO.appendStdout(jobId, TestHelpers.generateRandomByteObservable());
        final Path stdoutFile = jobsDir.resolve(jobId.toString()).resolve(Constants.STDOUT_FILENAME);

        Files.delete(stdoutFile);

        assertThat(filesystemJobsDAO.hasStdout(jobId)).isFalse();
    }



    @Test
    public void testHasStderrReturnsFalseIfTheStderrFileWasDeleted() throws IOException {
        final Path jobsDir = createTmpDir();
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);
        final JobId jobId = dao.persist(TestHelpers.STANDARD_VALID_REQUEST).getId();
        dao.appendStderr(jobId, TestHelpers.generateRandomByteObservable());
        final Path stderrFilePath = jobsDir.resolve(jobId.toString()).resolve(Constants.STDERR_FILENAME);

        Files.delete(stderrFilePath);

        assertThat(dao.hasStderr(jobId)).isFalse();
    }
}