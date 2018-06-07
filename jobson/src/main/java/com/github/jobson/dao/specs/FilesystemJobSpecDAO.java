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

import com.codahale.metrics.health.HealthCheck;
import com.github.jobson.Constants;
import com.github.jobson.specs.JobSpec;
import com.github.jobson.specs.JobSpecId;
import com.github.jobson.utils.DiskSpaceHealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.jobson.Constants.FILESYSTEM_SPECS_DAO_DISK_SPACE_HEALTHCHECK;
import static com.github.jobson.Constants.FILESYSTEM_SPECS_DAO_DISK_SPACE_WARNING_THRESHOLD_IN_BYTES;
import static com.github.jobson.Helpers.listDirectories;
import static com.github.jobson.Helpers.readYAML;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public final class FilesystemJobSpecDAO implements JobSpecDAO {

    private static final Logger log = LoggerFactory.getLogger(FilesystemJobSpecDAO.class);


    private static Optional<JobSpec> loadJobSpec(Path jobSpecDir) {
        final Path jobSpecPath = jobSpecDir.resolve(Constants.SPEC_DIR_SPEC_FILENAME);

        try {
            if (jobSpecPath.toFile().exists()) {
                final JobSpec jobSpec = readYAML(jobSpecPath.toFile(), JobSpec.class);
                jobSpec.setId(new JobSpecId(jobSpecDir.toFile().getName()));

                final JobSpec resolvedJobSpec = jobSpec.withDependenciesResolvedRelativeTo(jobSpecDir);
                return Optional.of(resolvedJobSpec);
            } else {
                log.error(jobSpecPath.toString() + ": does not exist");
                return Optional.empty();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
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
    public Optional<JobSpec> getJobSpecById(JobSpecId jobSpecId) {
        final Path jobSpecDir = jobSpecsDir.resolve(jobSpecId.toString());

        if (!jobSpecDir.toFile().exists()) {
            return Optional.empty();
        } else if (!jobSpecDir.toFile().isDirectory()) {
            log.error(jobSpecDir.toString() + ": is not a directory");
            return Optional.empty();
        } else {
            return loadJobSpec(jobSpecDir);
        }
    }

    @Override
    public Map<String, HealthCheck> getHealthChecks() {
        return singletonMap(
                FILESYSTEM_SPECS_DAO_DISK_SPACE_HEALTHCHECK,
                new DiskSpaceHealthCheck(
                        jobSpecsDir.toFile(),
                        FILESYSTEM_SPECS_DAO_DISK_SPACE_WARNING_THRESHOLD_IN_BYTES));
    }

    @Override
    public Optional<JobSpecSummary> getJobSpecSummaryById(JobSpecId jobSpecId) {
        return getJobSpecById(jobSpecId).map(JobSpec::toSummary);
    }

    @Override
    public List<JobSpecSummary> getJobSpecSummaries(int pageSize, int page) {
        return getJobSpecSummaries(pageSize, page, "");
    }

    @Override
    public List<JobSpecSummary> getJobSpecSummaries(int pageSize, int page, String query) {

        if (pageSize < 0)
            throw new IllegalArgumentException("pageSize is negative");
        if (page < 0)
            throw new IllegalArgumentException("page is negative");
        requireNonNull(query);

        return listDirectories(jobSpecsDir)
                .map(File::toPath)
                .map(path -> loadJobSpec(path)
                        .map(JobSpec::toSummary))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .skip(page * pageSize)
                .limit(pageSize)
                .sorted(FilesystemJobSpecDAO::bySpecName)
                .collect(toList());
    }

    private static int bySpecName(JobSpecSummary a, JobSpecSummary b) {
        return a.getName().compareTo(b.getName());
    }
}
