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

import com.github.jobson.Constants;
import com.github.jobson.Helpers;
import com.github.jobson.api.v1.JobSpecDetailsResponse;
import com.github.jobson.api.v1.JobSpecId;
import com.github.jobson.api.v1.JobSpecSummary;
import com.github.jobson.specs.ExecutionConfiguration;
import com.github.jobson.specs.JobDependencyConfiguration;
import com.github.jobson.specs.JobSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static com.github.jobson.Helpers.readYAML;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public final class FilesystemJobSpecDAO implements JobSpecDAO {

    private static final Logger log = LoggerFactory.getLogger(FilesystemJobSpecDAO.class);


    public static JobSpec resolveDependencySourcesRelativeTo(Path jobSpecDir, JobSpec jobSpec) {
        final Optional<List<JobDependencyConfiguration>> resolvedJobDependencies =
                jobSpec.getExecution().getDependencies().map(dependencies -> dependencies.stream().map(
                        dependency ->
                                new JobDependencyConfiguration(
                                        jobSpecDir.resolve(dependency.getSource()).toString(),
                                        dependency.getTarget())).collect(toList()));

        final ExecutionConfiguration executionConfiguration =
                new ExecutionConfiguration(
                        jobSpec.getExecution().getApplication(),
                        jobSpec.getExecution().getArguments(),
                        resolvedJobDependencies);

        return new JobSpec(
                jobSpec.getId(),
                jobSpec.getName(),
                jobSpec.getDescription(),
                jobSpec.getExpectedInputs(),
                executionConfiguration);
    }

    private static Optional<JobSpec> loadJobSpecConfiguration(Path jobSpecDir) {
        final Path jobSpecPath = jobSpecDir.resolve(Constants.JOB_SPEC_FILENAME);

        try {
            if (jobSpecPath.toFile().exists()) {
                final String jobSpecYAML = new String(Files.readAllBytes(jobSpecPath));
                final JobSpec jobSpec = readYAML(jobSpecYAML, JobSpec.class);
                jobSpec.setId(new JobSpecId(jobSpecDir.toFile().getName()));

                final JobSpec resolvedJobSpec = resolveDependencySourcesRelativeTo(jobSpecDir, jobSpec);
                return Optional.of(resolvedJobSpec);
            } else {
                log.error(jobSpecPath.toString() + ": does not exist");
                return Optional.empty();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static JobSpecSummary toSummary(JobSpec jobSpec) {
        return jobSpec.toSummary();
    }



    private final Path jobSpecsDir;



    public FilesystemJobSpecDAO(Path jobSpecsDir) throws IOException {
        requireNonNull(jobSpecsDir);
        if (!jobSpecsDir.toFile().exists())
            throw new FileNotFoundException(jobSpecsDir.toString() + ": No such directory");
        if (!jobSpecsDir.toFile().isDirectory())
            throw new NotDirectoryException(jobSpecsDir.toString() + ": Is not a directory");

        this.jobSpecsDir = jobSpecsDir;
    }


    @Override
    public Optional<JobSpec> getJobSpecConfigurationById(JobSpecId jobSpecId) {
        final Path jobSpecDir = jobSpecsDir.resolve(jobSpecId.toString());

        if (!jobSpecDir.toFile().exists()) {
            return Optional.empty();
        } else if (!jobSpecDir.toFile().isDirectory()) {
            log.error(jobSpecDir.toString() + ": is not a directory");
            return Optional.empty();
        } else {
            return loadJobSpecConfiguration(jobSpecDir);
        }
    }

    @Override
    public Optional<JobSpecDetailsResponse> getJobSpecDetailsById(JobSpecId jobSpecId) {
        return getJobSpecConfigurationById(jobSpecId).map(JobSpec::toAPIDetails);
    }

    @Override
    public List<JobSpecSummary> getJobSpecSummaries(int pageSize, int page) {
        return getJobSpecSummaries(pageSize, page, "");
    }

    @Override
    public List<JobSpecSummary> getJobSpecSummaries(int pageSize, int page, String query) {

        if (pageSize < 0) throw new IllegalArgumentException("pageSize is negative");
        if (page < 0) throw new IllegalArgumentException("page is negative");
        requireNonNull(query);

        return Helpers.listDirectories(jobSpecsDir)
                .map(File::toPath)
                .map(path -> loadJobSpecConfiguration(path).map(FilesystemJobSpecDAO::toSummary))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .skip(page * pageSize)
                .limit(pageSize)
                .collect(toList());
    }
}
