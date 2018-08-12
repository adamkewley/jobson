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

package com.github.jobson.dao.specs;

import com.github.jobson.TestHelpers;
import com.github.jobson.dao.jobs.FilesystemJobsDAOTest;
import com.github.jobson.specs.JobSpec;
import com.github.jobson.specs.JobSpecId;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static com.github.jobson.Constants.FILESYSTEM_SPECS_DAO_DISK_SPACE_HEALTHCHECK;
import static com.github.jobson.Constants.SPEC_DIR_SPEC_FILENAME;
import static com.github.jobson.Helpers.generateRandomBase36String;
import static com.github.jobson.TestHelpers.createTmpDir;
import static com.github.jobson.TestHelpers.readYAML;
import static io.dropwizard.testing.FixtureHelpers.fixture;
import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.createTempFile;
import static org.assertj.core.api.Assertions.assertThat;

public final class FilesystemJobSpecDAOTest {

    @Test(expected = NullPointerException.class)
    public void testCtorThrowsIfDirIsNull() throws IOException {
        new FilesystemJobSpecDAO(null);
    }

    @Test(expected = FileNotFoundException.class)
    public void testCtorThrowsIfDirDoesNotExist() throws IOException {
        new FilesystemJobSpecDAO(Paths.get(generateRandomBase36String(10)));
    }

    @Test(expected = NotDirectoryException.class)
    public void testCtorThrowsIfPathExistsButIsNotADirectory() throws IOException {
        final Path pathToFile = createTempFile(FilesystemJobSpecDAOTest.class.getSimpleName(), "");

        new FilesystemJobSpecDAO(pathToFile);
    }




    @Test
    public void testGetJobSpecDetailsByIdReturnsEmptyOptionalIfJobSpecIdDoesntExistInTheDir() throws IOException {
        final Path jobSpecsDir = createTmpDir(FilesystemJobSpecDAOTest.class);

        final FilesystemJobSpecDAO filesystemJobSpecDAO = new FilesystemJobSpecDAO(jobSpecsDir);

        final JobSpecId jobSpecId = new JobSpecId(generateRandomBase36String(10));

        final Optional<JobSpecSummary> jobSpecDetailsResponse =
                filesystemJobSpecDAO.getJobSpecSummaryById(jobSpecId);

        assertThat(jobSpecDetailsResponse.isPresent()).isFalse();
    }

    @Test
    public void testGetJobSpecDetailsByIdLoadsJobSpecDetailsFromTheDirectory() throws IOException {
        final Path jobSpecsDir = createTmpDir(FilesystemJobSpecDAOTest.class);

        final JobSpecId jobSpecId = new JobSpecId("test");
        final Path jobSpecPath = jobSpecsDir.resolve(jobSpecId.toString());

        createDirectory(jobSpecPath);

        final Path jobSpecConfigurationPath = jobSpecPath.resolve(SPEC_DIR_SPEC_FILENAME);
        final String jobSpecConfigurationText = fixture("fixtures/dao/specs/FilesystemBasedJobSpecDAO/valid-job-spec-configuration.yml");

        Files.write(jobSpecConfigurationPath, jobSpecConfigurationText.getBytes());

        final FilesystemJobSpecDAO filesystemJobSpecDAO = new FilesystemJobSpecDAO(jobSpecsDir);

        final Optional<JobSpec> maybeJobSpec =
                filesystemJobSpecDAO.getJobSpecById(jobSpecId);

        assertThat(maybeJobSpec).isPresent();

        final JobSpec jobSpec = maybeJobSpec.get();

        assertThat(jobSpec.getId()).isEqualTo(jobSpecId);

        final JobSpec originalJobSpec = readYAML(jobSpecConfigurationText, JobSpec.class);

        originalJobSpec.setId(jobSpecId);

        assertThat(jobSpec).isEqualTo(originalJobSpec.withDependenciesResolvedRelativeTo(jobSpecPath));
    }



    @Test
    public void testGetJobSpecConfigurationByIdReturnsEmptyOptionalIfJobSpecDoesNotExist() throws IOException {
        final Path jobSpecsDir = Files.createTempDirectory(FilesystemJobSpecDAOTest.class.getSimpleName());

        final FilesystemJobSpecDAO filesystemJobSpecDAO = new FilesystemJobSpecDAO(jobSpecsDir);

        final JobSpecId jobSpecId = new JobSpecId(generateRandomBase36String(10));

        final Optional<JobSpec> jobSpecDetailsResponse =
                filesystemJobSpecDAO.getJobSpecById(jobSpecId);

        assertThat(jobSpecDetailsResponse.isPresent()).isFalse();
    }

    @Test
    public void testGetJobSpecConfigurationByIdReturnsLoadsAJobSpecConfigurationFromTheDirectory() throws IOException {
        final Path jobSpecsDir = Files.createTempDirectory(FilesystemJobSpecDAOTest.class.getSimpleName());

        final JobSpecId jobSpecId = new JobSpecId("test");
        final Path jobSpecPath = jobSpecsDir.resolve(jobSpecId.toString());

        createDirectory(jobSpecPath);

        final Path jobSpecConfigurationPath = jobSpecPath.resolve(SPEC_DIR_SPEC_FILENAME);
        final String jobSpecConfigurationText =
                fixture("fixtures/dao/specs/FilesystemBasedJobSpecDAO/valid-job-spec-configuration-with-abs-path.yml");

        Files.write(jobSpecConfigurationPath, jobSpecConfigurationText.getBytes());

        final FilesystemJobSpecDAO filesystemJobSpecDAO = new FilesystemJobSpecDAO(jobSpecsDir);

        final Optional<JobSpec> maybeJobSpecConfiguration =
                filesystemJobSpecDAO.getJobSpecById(jobSpecId);

        assertThat(maybeJobSpecConfiguration.isPresent()).isTrue();

        final JobSpec jobSpec = maybeJobSpecConfiguration.get();

        final JobSpec expectedJobSpec =
                readYAML(jobSpecConfigurationText, JobSpec.class);

        expectedJobSpec.setId(jobSpecId);

        assertThat(jobSpec).isEqualTo(expectedJobSpec);
    }

    @Test
    public void testJobSpecConfigurationDependencyPathsAreResolvedRelativeToTheJobSpecFolderAsAbsolutePaths() throws IOException {
        final Path jobSpecsDir = Files.createTempDirectory(FilesystemJobSpecDAOTest.class.getSimpleName());

        final JobSpecId jobSpecId = new JobSpecId("test");
        final Path jobSpecPath = jobSpecsDir.resolve(jobSpecId.toString());

        createDirectory(jobSpecPath);

        final Path jobSpecConfigurationPath = jobSpecPath.resolve(SPEC_DIR_SPEC_FILENAME);
        final String jobSpecConfigurationText = fixture("fixtures/dao/specs/FilesystemBasedJobSpecDAO/valid-job-spec-configuration.yml");

        Files.write(jobSpecConfigurationPath, jobSpecConfigurationText.getBytes());

        final FilesystemJobSpecDAO filesystemJobSpecDAO = new FilesystemJobSpecDAO(jobSpecsDir);

        final Optional<JobSpec> maybeJobSpecConfiguration =
                filesystemJobSpecDAO.getJobSpecById(jobSpecId);

        assertThat(maybeJobSpecConfiguration.isPresent()).isTrue();

        final JobSpec jobSpec = maybeJobSpecConfiguration.get();

        final String source =
                jobSpec.getExecution().getDependencies().get().get(0).getSource().getValue();

        assertThat(source).isNotEqualTo("libyaml.so");
        assertThat(source).isEqualTo(jobSpecPath.resolve("libyaml.so").toString());
    }




    // .getJobSpecSummaries (with query)

    @Test(expected = IllegalArgumentException.class)
    public void testGetJobSpecSummariesWithQueryThrowsInvalidArgumentExceptionIfPageSizeIsNegative() throws IOException {
        final Path jobSpecsDir = Files.createTempDirectory(FilesystemJobSpecDAOTest.class.getSimpleName());
        final FilesystemJobSpecDAO filesystemJobSpecDAO = new FilesystemJobSpecDAO(jobSpecsDir);

        filesystemJobSpecDAO.getJobSpecSummaries(-1, 0, TestHelpers.generateRandomString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetJobSpecSummariesWithQueryThrowsInvalidArgumentExceptionIfPageIsNegative() throws IOException {
        final Path jobSpecsDir = Files.createTempDirectory(FilesystemJobSpecDAOTest.class.getSimpleName());
        final FilesystemJobSpecDAO filesystemJobSpecDAO = new FilesystemJobSpecDAO(jobSpecsDir);

        filesystemJobSpecDAO.getJobSpecSummaries(20, -1, TestHelpers.generateRandomString());
    }

    @Test(expected = NullPointerException.class)
    public void testGetJobSpecSummariesWithQueryThrowsNPEIfQueryIsNull() throws IOException {
        final Path jobSpecsDir = Files.createTempDirectory(FilesystemJobSpecDAOTest.class.getSimpleName());
        final FilesystemJobSpecDAO filesystemJobSpecDAO = new FilesystemJobSpecDAO(jobSpecsDir);

        filesystemJobSpecDAO.getJobSpecSummaries(20, 0, null);
    }

    @Test
    public void testGetJobSpecSummariesWithQueryReturnsAList() throws IOException {
        final Path jobSpecsDir = Files.createTempDirectory(FilesystemJobSpecDAOTest.class.getSimpleName());
        final FilesystemJobSpecDAO filesystemJobSpecDAO = new FilesystemJobSpecDAO(jobSpecsDir);

        final List<JobSpecSummary> jobSpecSummaries =
                filesystemJobSpecDAO.getJobSpecSummaries(20, 0, "");

        assertThat(jobSpecSummaries).isNotNull();
    }

    @Test
    public void testGetJobSpecSummariesWithQueryReturnsOnlyTheNumberOfEntriesSpecifiedByPageSize() throws IOException {
        final Path jobSpecsDir = Files.createTempDirectory(FilesystemJobSpecDAOTest.class.getSimpleName());

        final int pageSize = 10;
        final int numberOfSpecs = pageSize * 3;
        final String jobSpecConfigurationText = fixture("fixtures/dao/specs/FilesystemBasedJobSpecDAO/valid-job-spec-configuration.yml");

        for (int i = 0; i < numberOfSpecs; i++) {
            final JobSpecId jobSpecId = new JobSpecId(generateRandomBase36String(5));
            final Path jobSpecPath = jobSpecsDir.resolve(jobSpecId.toString());

            createDirectory(jobSpecPath);

            final Path jobSpecConfigurationPath = jobSpecPath.resolve(SPEC_DIR_SPEC_FILENAME);

            Files.write(jobSpecConfigurationPath, jobSpecConfigurationText.getBytes());
        }

        final FilesystemJobSpecDAO filesystemJobSpecDAO = new FilesystemJobSpecDAO(jobSpecsDir);

        final List<JobSpecSummary> jobSpecSummaries =
                filesystemJobSpecDAO.getJobSpecSummaries(pageSize, 0, "");

        assertThat(jobSpecSummaries.size()).isEqualTo(pageSize);
    }




    // .getJobSpecSummaries (without query)

    @Test(expected = IllegalArgumentException.class)
    public void testGetJobSpecSummariesWithoutQueryThrowsInvalidArgumentExceptionIfPageSizeIsNegative() throws IOException {
        final Path jobSpecsDir = Files.createTempDirectory(FilesystemJobSpecDAOTest.class.getSimpleName());
        final FilesystemJobSpecDAO filesystemJobSpecDAO = new FilesystemJobSpecDAO(jobSpecsDir);

        filesystemJobSpecDAO.getJobSpecSummaries(-1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetJobSpecSummariesWithoutQueryThrowsInvalidArgumentExceptionIfPageIsNegative() throws IOException {
        final Path jobSpecsDir = Files.createTempDirectory(FilesystemJobSpecDAOTest.class.getSimpleName());
        final FilesystemJobSpecDAO filesystemJobSpecDAO = new FilesystemJobSpecDAO(jobSpecsDir);

        filesystemJobSpecDAO.getJobSpecSummaries(20, -1);
    }

    @Test
    public void testGetJobSpecSummariesWithoutQueryReturnsAList() throws IOException {
        final Path jobSpecsDir = Files.createTempDirectory(FilesystemJobSpecDAOTest.class.getSimpleName());
        final FilesystemJobSpecDAO filesystemJobSpecDAO = new FilesystemJobSpecDAO(jobSpecsDir);

        final List<JobSpecSummary> jobSpecSummaries =
                filesystemJobSpecDAO.getJobSpecSummaries(20, 0);

        assertThat(jobSpecSummaries).isNotNull();
    }

    @Test
    public void testGetJobSpecSummariesWithoutQueryReturnsOnlyTheNumberOfEntriesSpecifiedByPageSize() throws IOException {
        final Path jobSpecsDir = Files.createTempDirectory(FilesystemJobSpecDAOTest.class.getSimpleName());

        final int pageSize = 10;
        final int numberOfSpecs = pageSize * 3;
        final String jobSpecConfigurationText = fixture("fixtures/dao/specs/FilesystemBasedJobSpecDAO/valid-job-spec-configuration.yml");

        for (int i = 0; i < numberOfSpecs; i++) {
            final JobSpecId jobSpecId = new JobSpecId(generateRandomBase36String(5));
            final Path jobSpecPath = jobSpecsDir.resolve(jobSpecId.toString());

            createDirectory(jobSpecPath);

            final Path jobSpecConfigurationPath = jobSpecPath.resolve(SPEC_DIR_SPEC_FILENAME);

            Files.write(jobSpecConfigurationPath, jobSpecConfigurationText.getBytes());
        }

        final FilesystemJobSpecDAO filesystemJobSpecDAO = new FilesystemJobSpecDAO(jobSpecsDir);

        final List<JobSpecSummary> jobSpecSummaries =
                filesystemJobSpecDAO.getJobSpecSummaries(pageSize, 0);

        assertThat(jobSpecSummaries.size()).isEqualTo(pageSize);
    }

    @Test
    public void testGetHealthChecksReturnsHealthCheckThatTestsDiskSpace() throws IOException {
        final FilesystemJobSpecDAO dao = new FilesystemJobSpecDAO(createTmpDir(FilesystemJobsDAOTest.class));
        assertThat(dao.getHealthChecks()).containsKeys(FILESYSTEM_SPECS_DAO_DISK_SPACE_HEALTHCHECK);
        assertThat(dao.getHealthChecks().get(FILESYSTEM_SPECS_DAO_DISK_SPACE_HEALTHCHECK)).isNotNull();
    }
}