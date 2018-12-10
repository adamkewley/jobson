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
import com.github.jobson.dao.IdGenerator;
import com.github.jobson.jobs.JobId;
import com.github.jobson.jobs.JobOutput;
import com.github.jobson.jobs.jobstates.PersistedJob;
import com.github.jobson.jobs.jobstates.ValidJobRequest;
import com.github.jobson.specs.JobOutputId;
import com.github.jobson.specs.JobSpec;
import com.github.jobson.utils.BinaryData;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static com.github.jobson.Constants.*;
import static com.github.jobson.Helpers.*;
import static com.github.jobson.Helpers.readJSON;
import static com.github.jobson.TestHelpers.*;
import static org.assertj.core.api.Assertions.assertThat;

public final class FilesystemJobsDAOTest extends JobsDAOTest {

    private static FilesystemJobsDAO createStandardFilesystemDAO() {
        try {
            final Path jobDir = createTmpDir(FilesystemJobsDAOTest.class);
            return createStandardFilesystemDAO(jobDir);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static FilesystemJobsDAO createStandardFilesystemDAO(Path jobDir) throws IOException {
        return new FilesystemJobsDAO(jobDir, createIdGenerator());
    }

    private static IdGenerator createIdGenerator() {
        return () -> generateRandomBase36String(10);
    }



    @Override
    protected JobDAO getInstance() {
        return createStandardFilesystemDAO();
    }



    @Test(expected = NullPointerException.class)
    public void testCtorThrowsIfPassedNulls() throws IOException {
        final Path jobDir = createTmpDir(FilesystemJobsDAOTest.class);
        final IdGenerator idGenerator = createIdGenerator();

        new FilesystemJobsDAO(jobDir, null);
        new FilesystemJobsDAO(null, idGenerator);
    }

    @Test(expected = FileNotFoundException.class)
    public void testCtorThrowsIfPassedANonExistentJobsDir() throws IOException {
        final Path invalidPath = Paths.get(generateAlphanumStr());
        createStandardFilesystemDAO(invalidPath);
    }



    @Test
    public void testPersistNewJobCreatesAJobDirNamedWithTheJobID() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final JobId jobId = persistValidRequest(jobsDir).getId();
        assertThat(tryResolve(jobsDir, jobId)).isPresent();
    }

    private PersistedJob persistValidRequest(Path jobsDir) throws IOException {
        return persistRequest(jobsDir, STANDARD_VALID_REQUEST);
    }

    private PersistedJob persistRequest(Path jobsDir, ValidJobRequest jobRequest) throws IOException {
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);
        return dao.persist(jobRequest);
    }

    @Test
    public void testPersistNewJobJobDirectoryContainsAJobRequestJSONFile() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final JobId jobId = persistValidRequest(jobsDir).getId();
        assertThat(tryResolve(jobsDir, jobId, JOB_DIR_JOB_DETAILS_FILENAME)).isPresent();
    }

    @Test
    public void testPersistNewJobJobDirectoryJobRequestJSONFileIsValidJSON() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final JobId jobId = persistValidRequest(jobsDir).getId();
        readJSON(tryResolve(jobsDir, jobId, JOB_DIR_JOB_DETAILS_FILENAME).get(), Object.class);
    }

    @Test
    public void testPersistNewJobDirJobRequestJSONFileIsJobDetails() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final JobId jobId = persistValidRequest(jobsDir).getId();
        readJSON(tryResolve(jobsDir, jobId, JOB_DIR_JOB_DETAILS_FILENAME).get(), JobDetails.class);
    }


    @Test
    public void testPersistNewJobJobDirectoryContainsTheJobsSchema() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final JobId jobId = persistValidRequest(jobsDir).getId();
        assertThat(tryResolve(jobsDir, jobId, Constants.JOB_DIR_JOB_SPEC_FILENAME)).isPresent();
    }

    @Test
    public void testPersistNewJobJobDirectorySchemaFileIsValidJSON() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final JobId jobId = persistValidRequest(jobsDir).getId();
        assertThat(Helpers.tryResolve(jobsDir, jobId, Constants.JOB_DIR_JOB_SPEC_FILENAME)).isPresent();
    }


    @Test
    public void testPersistNewJobJobDirectoryJSONParsesToAJobSchemaConfiguration() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final JobId jobId = persistValidRequest(jobsDir).getId();
        readJSON(tryResolve(jobsDir, jobId, Constants.JOB_DIR_JOB_SPEC_FILENAME).get(), JobSpec.class);
    }



    @Test
    public void testHasStdoutReturnsFalseIfTheStdoutFileWasDeleted() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO filesystemJobsDAO = createStandardFilesystemDAO(jobsDir);
        final JobId jobId = filesystemJobsDAO.persist(STANDARD_VALID_REQUEST).getId();
        filesystemJobsDAO.appendStdout(jobId, TestHelpers.generateRandomByteObservable());
        final Path stdoutFile = jobsDir.resolve(jobId.toString()).resolve(Constants.JOB_DIR_STDOUT_FILENAME);

        Files.delete(stdoutFile);

        assertThat(filesystemJobsDAO.hasStdout(jobId)).isFalse();
    }



    @Test
    public void testHasStderrReturnsFalseIfTheStderrFileWasDeleted() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        dao.appendStderr(jobId, TestHelpers.generateRandomByteObservable());
        final Path stderrFilePath = jobsDir.resolve(jobId.toString()).resolve(Constants.JOB_DIR_STDERR_FILENAME);

        Files.delete(stderrFilePath);

        assertThat(dao.hasStderr(jobId)).isFalse();
    }

    @Test
    public void testGetSpecJobWasSubmittedAgainstReturnsOptionalEmptyIfJobDoesntExist() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);
        assertThat(dao.getSpecJobWasSubmittedAgainst(generateJobId())).isNotPresent();
    }

    @Test
    public void testGetSpecJobWasSubmittedAgainstReturnsSpecIfJobDoesExist() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        final Optional<JobSpec> maybeJobSpec = dao.getSpecJobWasSubmittedAgainst(jobId);

        assertThat(maybeJobSpec).isPresent();
        assertThat(maybeJobSpec.get()).isEqualTo(STANDARD_VALID_REQUEST.getSpec());
    }


    @Test
    public void testPersistJobOutputOutputFolderDoesNotExistBeforePersisting() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);

        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();

        assertThat(jobsDir.resolve(jobId.toString()).resolve(JOB_DIR_OUTPUTS_DIRNAME)).doesNotExist();
    }

    @Test(expected = RuntimeException.class)
    public void testPersistJobOutputThrowsIfJobDoesNotExist() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);

        dao.persistOutput(generateJobId(), generateRandomJobOutput());
    }

    @Test
    public void testPersistJobOutputSavesTheJobOutputToAnOutputsSubfolder() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);

        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();

        final byte[] data = generateRandomBytes();
        final JobOutput jobOutput = generateRandomJobOutput(data);

        dao.persistOutput(jobId, jobOutput);

        final Path outputsDir = jobsDir.resolve(jobId.toString()).resolve(JOB_DIR_OUTPUTS_DIRNAME);

        assertThat(outputsDir.toFile()).exists();

        final Path outputFile = outputsDir.resolve(jobOutput.getId().toString());

        assertThat(outputFile).exists();

        final byte[] outputFileContent = Files.readAllBytes(outputFile);

        assertThat(outputFileContent).isEqualTo(data);
    }

    @Test
    public void testPersistJobOutputOverwritesExistingOutputWithSameId() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);

        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();

        final JobOutput firstJobOutput = generateRandomJobOutput();
        final JobOutputId outputId = firstJobOutput.getId();

        dao.persistOutput(jobId, firstJobOutput);

        final byte secondJobData[] = generateRandomBytes();
        final JobOutput secondJobOutput = generateRandomJobOutput(firstJobOutput.getId(), secondJobData);

        dao.persistOutput(jobId, secondJobOutput);

        final Path outputsDir = jobsDir.resolve(jobId.toString()).resolve(JOB_DIR_OUTPUTS_DIRNAME);

        assertThat(outputsDir.toFile()).exists();

        final Path outputFile = outputsDir.resolve(outputId.toString());

        assertThat(outputFile).exists();

        final byte[] outputFileContent = Files.readAllBytes(outputFile);

        assertThat(outputFileContent).isEqualTo(secondJobData);
    }

    @Test
    public void testGetHealthChecksReturnsHealthChecksForRemainingDiskSpace() {
        final FilesystemJobsDAO dao = createStandardFilesystemDAO();
        assertThat(dao.getHealthChecks()).containsKey(FILESYSTEM_JOBS_DAO_DISK_SPACE_HEALTHCHECK);
        assertThat(dao.getHealthChecks().get(FILESYSTEM_JOBS_DAO_DISK_SPACE_HEALTHCHECK)).isNotNull();
    }

    @Test
    public void testHasJobInputsReturnsFalseIfJobExistsButInputsWereDeleted() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);
        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();

        final Path pathToInputsJSONFile = jobsDir.resolve(jobId.toString()).resolve(Constants.JOB_DIR_JOB_INPUTS_FILENAME);

        Files.delete(pathToInputsJSONFile);

        assertThat(dao.hasJobInputs(jobId)).isFalse();
    }

    @Test
    public void testGetJobsDoesNotThrowWhenANoneJobDirectoryIsInTheJobsDirectory() throws IOException {
        // This is to ensure the jobs dir is robust to the presence of other dirs (e.g. `.git`)

        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);

        dao.persist(STANDARD_VALID_REQUEST).getId();
        Files.createDirectory(jobsDir.resolve(".git"));

        final List<JobDetails> jobs = dao.getJobs(100, 0);

        assertThat(jobs.size()).isEqualTo(1);
    }

    @Test
    public void testGetJobsDoesNotThrowWhenAnArbitraryFileIsInTheJobsDirectory() throws IOException {
        // This is to ensure the jobs dir is robust to the presence of files (e.g. `README.md`)

        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);

        dao.persist(STANDARD_VALID_REQUEST).getId();
        Files.createFile(jobsDir.resolve("README.md"));

        final List<JobDetails> jobs = dao.getJobs(100, 0);

        assertThat(jobs.size()).isEqualTo(1);
    }

    @Test
    public void testGetJobsDoesNotThrowWhenAJobDirectoryDoesntContainARequestJson() throws IOException {
        // This is to ensure the jobs dir is robust to the initial request.json being deleted (e.g.
        // by an external garbage collector). This is because some filesystems (e.g. NFS) can leave
        // remnants of files around a little while after deletion.

        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);

        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        Files.delete(jobsDir.resolve(jobId.toString()).resolve(Constants.JOB_DIR_JOB_DETAILS_FILENAME));

        final List<JobDetails> jobs = dao.getJobs(100, 0);

        assertThat(jobs.size()).isEqualTo(0);
    }

    @Test
    public void testGetJobsDoesNotThrowWhenASpecIsDeletedFromAJobDirectory() throws IOException {
        // This is to ensure the jobs dir is robust to deleting the spec.json, which isn't *strictly*
        // necessary for persisting the job

        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);

        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        Files.delete(jobsDir.resolve(jobId.toString()).resolve(Constants.JOB_DIR_JOB_SPEC_FILENAME));

        final List<JobDetails> jobs = dao.getJobs(100, 0);

        assertThat(jobs.size()).isEqualTo(1);
    }

    @Test
    public void testGetJobSpecJustReturnsEmptyOptionalIfDeletedFromAJobDirectory() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);

        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        Files.delete(jobsDir.resolve(jobId.toString()).resolve(Constants.JOB_DIR_JOB_SPEC_FILENAME));

        final Optional<JobSpec> maybeSpec = dao.getSpecJobWasSubmittedAgainst(jobId);

        assertThat(maybeSpec).isNotPresent();
    }

    @Test
    public void testGetJobJobsDoesNotThrowWhenInputsAreDeletedFromJobDirectory() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);

        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        Files.delete(jobsDir.resolve(jobId.toString()).resolve(Constants.JOB_DIR_JOB_INPUTS_FILENAME));

        final List<JobDetails> jobs = dao.getJobs(100, 0);

        assertThat(jobs.size()).isEqualTo(1);
    }

    @Test
    public void testGetJobsDoesNotThrowWhenStdoutIsDeletedFromJobDirectory() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);

        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        dao.appendStdout(jobId, TestHelpers.generateRandomByteObservable());
        Files.delete(jobsDir.resolve(jobId.toString()).resolve(Constants.JOB_DIR_STDOUT_FILENAME));

        final List<JobDetails> jobs = dao.getJobs(100, 0);

        assertThat(jobs.size()).isEqualTo(1);
    }

    @Test
    public void testGetJobsDoesNotThrowWhenStderrIsDeletedFromJobDirectory() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);

        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        dao.appendStderr(jobId, TestHelpers.generateRandomByteObservable());
        Files.delete(jobsDir.resolve(jobId.toString()).resolve(Constants.JOB_DIR_STDERR_FILENAME));

        final List<JobDetails> jobs = dao.getJobs(100, 0);

        assertThat(jobs.size()).isEqualTo(1);
    }

    @Test
    public void testGetJobOutputsDoesntFailIfOutputDeletedButStillInMetadata() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);

        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();
        final JobOutput persistedOutput = generateRandomJobOutput();
        dao.persistOutput(jobId, persistedOutput);

        Files.delete(
                jobsDir.resolve(jobId.toString())
                        .resolve(Constants.JOB_DIR_OUTPUTS_DIRNAME)
                        .resolve(persistedOutput.getId().toString()));

        final List<JobOutputDetails> jobOutputs = dao.getJobOutputs(jobId);

        assertThat(jobOutputs.size()).isEqualTo(1); // Because there's metadata for it

        final Optional<BinaryData> data = dao.getOutput(jobId, persistedOutput.getId());

        assertThat(data).isNotPresent();  // Because the data itself has been deleted
    }

    @Test
    public void testRemoveDeletesTheJobsDirectory() throws IOException {
        final Path jobsDir = createTmpDir(FilesystemJobsDAOTest.class);
        final FilesystemJobsDAO dao = createStandardFilesystemDAO(jobsDir);

        final JobId jobId = dao.persist(STANDARD_VALID_REQUEST).getId();

        assertThat(Files.exists(jobsDir.resolve(jobId.toString()))).isTrue();

        dao.remove(jobId);

        assertThat(Files.exists(jobsDir.resolve(jobId.toString()))).isFalse();
    }
}