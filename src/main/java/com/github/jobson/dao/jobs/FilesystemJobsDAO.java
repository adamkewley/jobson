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

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.jobson.Helpers;
import com.github.jobson.api.v1.JobId;
import com.github.jobson.api.v1.JobStatus;
import com.github.jobson.api.v1.JobTimestamp;
import com.github.jobson.dao.BinaryData;
import com.github.jobson.dao.IdGenerator;
import com.github.jobson.jobs.states.PersistedJobRequest;
import com.github.jobson.jobs.states.ValidJobRequest;
import com.github.jobson.specs.JobOutput;
import com.github.jobson.specs.JobSpec;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.github.jobson.Constants.*;
import static com.github.jobson.Helpers.*;
import static com.github.jobson.dao.jobs.JobDetails.fromPersistedJob;
import static java.lang.String.format;
import static java.nio.file.Files.createDirectory;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


/**
 * Persists all job data (query, spec, dependencies, stdout, stderr etc.)
 * in a directory on the filesystem.
 */
public final class FilesystemJobsDAO implements JobDAO {

    private static final Logger log = Logger.getLogger(FilesystemJobsDAO.class);


    private final Object fsLock = new Object();
    private final Path jobsDirectory;
    private final IdGenerator idGenerator;


    public FilesystemJobsDAO(Path jobsDirectory, IdGenerator idGenerator)
            throws NullPointerException, FileNotFoundException {

        requireNonNull(jobsDirectory);
        requireNonNull(idGenerator);

        if (!Files.exists(jobsDirectory))
            throw new FileNotFoundException(jobsDirectory + ": Does not exist");

        this.jobsDirectory = jobsDirectory;
        this.idGenerator = idGenerator;
    }


    @Override
    public boolean jobExists(JobId jobId) {
        return resolveJobDir(jobId).isPresent();
    }

    private Optional<Path> resolveJobDir(JobId jobId) {
        return tryResolve(jobsDirectory, jobId.toString());
    }

    private Optional<Path> resolveJobFile(JobId jobId, String filename) {
        return tryResolve(jobsDirectory, jobId.toString(), filename);
    }

    @Override
    public List<JobDetails> getJobs(int pageSize, int page) {
        return getJobs(pageSize, page, "");
    }

    @Override
    public List<JobDetails> getJobs(int pageSize, int pageNumber, String query) {

        if (pageSize < 0) throw new IllegalArgumentException("pageSize is below 0");
        if (pageNumber < 0) throw new IllegalArgumentException("page is below 0");
        if (query == null) throw new IllegalArgumentException("query string is null");

        final Predicate<JobDetails> resultFilter = jobDetails -> {
            final String allFields =
                    jobDetails.getName() + jobDetails.getOwner() + jobDetails.getId();
            return allFields.toLowerCase().contains(query.toLowerCase());
        };

        return loadAllJobs()
                .filter(resultFilter)
                .sorted(FilesystemJobsDAO::byFirstStatusDate)
                .skip(pageSize * pageNumber)
                .limit(pageSize)
                .collect(toList());
    }

    private Stream<JobDetails> loadAllJobs() {
        synchronized (fsLock) {
            return listDirectories(jobsDirectory).map(this::loadJobDetails);
        }
    }

    private JobDetails loadJobDetails(File jobDir) {
        final Path jobDetailsPath = jobDir.toPath().resolve(JOB_DIR_JOB_DETAILS_FILENAME);
        try {
            return loadJSON(jobDetailsPath, JobDetails.class);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static int byFirstStatusDate(JobDetails a, JobDetails b) {
        return lastElement(b.getTimestamps()).get().getTime().compareTo(
                lastElement(a.getTimestamps()).get().getTime());
    }

    @Override
    public Disposable appendStdout(JobId jobId, Observable<byte[]> stdout) {
        return persistObservableToJobFile(jobId, JOB_DIR_STDOUT_FILENAME, stdout);
    }

    private Disposable persistObservableToJobFile(JobId jobId, String jobFilename, Observable<byte[]> o) {
        if (!resolveJobDir(jobId).isPresent())
            throw new RuntimeException(jobId + ": cannot persist stdout: job dir does not exist");

        final Path stdoutPath = resolveJobDir(jobId).get().resolve(jobFilename);

        try {
            final FileOutputStream outputStream = new FileOutputStream(stdoutPath.toFile());
            return o.subscribe(outputStream::write, error -> outputStream.close(), outputStream::close);
        } catch (IOException ex) {
            throw new RuntimeException(jobId + ": cannot persist stdout: " + ex.toString());
        }
    }

    @Override
    public Disposable appendStderr(JobId jobId, Observable<byte[]> stderr) {
        return persistObservableToJobFile(jobId, JOB_DIR_STDERR_FILENAME, stderr);
    }

    @Override
    public PersistedJobRequest persist(ValidJobRequest validJobRequest) {
        final JobId jobId = generateUniqueJobId();

        final PersistedJobRequest persistedJobRequest =
                PersistedJobRequest.createFromValidRequest(validJobRequest, jobId);

        createNewJobDirectory(persistedJobRequest);

        return persistedJobRequest;
    }

    private JobId generateUniqueJobId() {
        for(int i = 0; i < MAX_JOB_ID_GENERATION_ATTEMPTS; i++) {
            final String id = idGenerator.generate();
            if (!Files.exists(this.jobsDirectory.resolve(id))) {
                return new JobId(id);
            }
        }

        final String errorMsg = format(
                "Could not generate unique job ID after %s attempts", MAX_JOB_ID_GENERATION_ATTEMPTS);
        log.error(errorMsg);
        throw new RuntimeException(errorMsg);
    }

    private void createNewJobDirectory(PersistedJobRequest persistedJobRequest) {
        final JobId id = persistedJobRequest.getId();
        try {
            final Path jobDir = jobsDirectory.resolve(id.toString());
            createDirectory(jobDir);
            log.debug(id + ": created job dir: " + jobDir);

            final Path jobSpecPath = jobDir.resolve(JOB_DIR_JOB_SPEC_FILENAME);
            writeJSON(jobSpecPath, persistedJobRequest.getSpec());
            log.debug(id + ": written spec file: " + jobSpecPath);

            final Path jobDetailsPath = jobDir.resolve(JOB_DIR_JOB_DETAILS_FILENAME);
            writeJSON(jobDetailsPath, fromPersistedJob(persistedJobRequest));
            log.debug(id + ": written job details: " + jobDetailsPath);
        } catch (IOException ex) {
            log.error(id + ": could not setup job directory: " + ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addNewJobStatus(JobId jobId, JobStatus newStatus, String statusMessage) {
        if (!resolveJobDir(jobId).isPresent())
            throw new RuntimeException(jobId + ": cannot add status: job does not exist");

        synchronized (fsLock) {
            final Optional<Path> maybeJobDetailsPath =
                    resolveJobFile(jobId, JOB_DIR_JOB_DETAILS_FILENAME);

            if (maybeJobDetailsPath.isPresent()) {
                try {
                    final Path jobDetailsPath = maybeJobDetailsPath.get();
                    final JobDetails jobDetails = readJSON(jobDetailsPath, JobDetails.class);
                    final JobTimestamp newTimestamp = JobTimestamp.now(newStatus, statusMessage);
                    final JobDetails updatedJobDetails = jobDetails.withStatusChangeTimestamp(newTimestamp);
                    writeJSON(jobDetailsPath, updatedJobDetails);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                throw new RuntimeException(jobId + ": cannot add new status: " + JOB_DIR_JOB_DETAILS_FILENAME + " does not exist");
            }
        }
    }

    @Override
    public void persistOutput(JobId jobId, String outputId, BinaryData data) {
        final Optional<Path> maybeJobDir = resolveJobDir(jobId);

        if (!maybeJobDir.isPresent())
            throw new RuntimeException(outputId + ": cannot be persisted to job " + jobId + ": job dir does not exist");

        final Path outputsDir = maybeJobDir.get().resolve(JOB_DIR_OUTPUTS_DIRNAME);
        final Path outputPath = outputsDir.resolve(outputId);

        if (!outputsDir.toFile().exists()) {
            try {
                Files.createDirectory(outputsDir);
            } catch (IOException ex) {
                throw new RuntimeException(outputsDir + ": cannot be created: " + ex);
            }
        }

        try {
            IOUtils.copy(data.getData(), new FileOutputStream(outputPath.toFile(), false));
        } catch (IOException ex) {
            throw new RuntimeException(outputPath + ": cannot write: " + ex);
        }

        final Optional<Path> maybeJobOutputsFile = resolveJobFile(jobId, JOB_DIR_OUTPUTS_FILENAME);

        if (maybeJobOutputsFile.isPresent()) {
            final Path jobOutputsFile = maybeJobOutputsFile.get();
            try {
                final Map<String, JobOutput> loadedOutputs =
                        loadJSON(jobOutputsFile,  new TypeReference<Map<String, JobOutput>>(){});
                loadedOutputs.put(outputId, new JobOutput(outputId, data.getMimeType()));
                writeJSON(jobOutputsFile, loadedOutputs);
            } catch (IOException ex) {
                throw new RuntimeException(jobOutputsFile + ": cannot write");
            }
        } else {
            try {
                final Map<String, JobOutput> outputs = new HashMap<>();
                outputs.put(outputId, new JobOutput(outputId, data.getMimeType()));
                writeJSON(resolveJobDir(jobId).get().resolve(JOB_DIR_OUTPUTS_FILENAME), outputs);
            } catch (IOException ex) {
                throw new RuntimeException("Cannot write job outputs file for " + jobId);
            }
        }
    }

    @Override
    public Optional<JobDetails> getJobDetailsById(JobId jobId) {
        synchronized (fsLock) {
            return resolveJobDir(jobId)
                    .map(Path::toFile)
                    .map(this::loadJobDetails);
        }
    }

    @Override
    public Optional<JobSpec> getSpecJobWasSubmittedAgainst(JobId jobId) {
        return resolveJobDir(jobId).map(Path::toFile).map(this::loadJobSpec);
    }

    private JobSpec loadJobSpec(File jobDir) {
        final Path jobDetailsPath = jobDir.toPath().resolve(JOB_DIR_JOB_SPEC_FILENAME);
        try {
            return loadJSON(jobDetailsPath, JobSpec.class);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


    @Override
    public boolean hasStderr(JobId jobId) {
        return resolveJobFile(jobId, JOB_DIR_STDERR_FILENAME).isPresent();
    }

    @Override
    public Optional<BinaryData> getStderr(JobId jobId) {
        return resolveJobFile(jobId, JOB_DIR_STDERR_FILENAME).map(Helpers::streamBinaryData);
    }

    @Override
    public Set<JobId> getJobsWithStatus(JobStatus status) {
        return loadAllJobs()
                .filter(details -> details.latestStatus().equals(status))
                .map(JobDetails::getId)
                .collect(toSet());
    }

    @Override
    public boolean hasOutput(JobId jobId, String outputId) {
        return tryResolveOutput(jobId, outputId).isPresent();
    }

    private Optional<Path> tryResolveOutput(JobId jobId, String outputId) {
        return resolveJobDir(jobId).flatMap(p -> tryResolve(p, JOB_DIR_OUTPUTS_DIRNAME, outputId));
    }

    @Override
    public Optional<BinaryData> getOutput(JobId jobId, String outputId) {
        return resolveJobFile(jobId, JOB_DIR_OUTPUTS_FILENAME)
                .map(p -> {
                    try {
                        return loadJSON(p, new TypeReference<Map<String, JobOutput>>(){});
                    } catch (IOException ex) {
                        throw new RuntimeException(jobId + ": " + JOB_DIR_OUTPUTS_FILENAME + ": cannot parse");
                    }
                })
                .flatMap(m -> tryGet(m, outputId))
                .flatMap(jobOutput ->
                        tryResolveOutput(jobId, jobOutput.getPath())
                                .map(Helpers::streamBinaryData)
                                .map(b -> b.withMimeType(jobOutput.getMimeType())));
    }

    @Override
    public Map<String, JobOutput> getJobOutputs(JobId jobId) {
        return resolveJobFile(jobId, JOB_DIR_OUTPUTS_FILENAME)
                .map(outputsFile -> {
                    try {
                        return loadJSON(outputsFile, new TypeReference<Map<String, JobOutput>>(){});
                    } catch (IOException ex) {
                        throw new RuntimeException(outputsFile + ": cannot deserialize: " + ex);
                    }
                })
                .orElse(new HashMap<>());
    }

    @Override
    public boolean hasStdout(JobId jobId) {
        return resolveJobFile(jobId, JOB_DIR_STDOUT_FILENAME).isPresent();
    }

    @Override
    public Optional<BinaryData> getStdout(JobId jobId) {
        return resolveJobFile(jobId, JOB_DIR_STDOUT_FILENAME).map(Helpers::streamBinaryData);
    }
}
