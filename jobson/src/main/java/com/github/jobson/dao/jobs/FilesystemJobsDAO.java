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

import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.jobson.Helpers;
import com.github.jobson.dao.IdGenerator;
import com.github.jobson.jobinputs.JobExpectedInputId;
import com.github.jobson.jobs.JobId;
import com.github.jobson.jobs.JobOutput;
import com.github.jobson.jobs.JobStatus;
import com.github.jobson.jobs.JobTimestamp;
import com.github.jobson.jobs.jobstates.PersistedJob;
import com.github.jobson.jobs.jobstates.ValidJobRequest;
import com.github.jobson.specs.JobOutputId;
import com.github.jobson.specs.JobSpec;
import com.github.jobson.utils.BinaryData;
import com.github.jobson.utils.DiskSpaceHealthCheck;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.github.jobson.Constants.*;
import static com.github.jobson.Helpers.*;
import static com.github.jobson.dao.jobs.JobDetails.fromPersistedJob;
import static java.lang.String.format;
import static java.nio.file.Files.createDirectory;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Persists all job data (query, spec, dependencies, stdout, stderr etc.)
 * in a directory on the filesystem.
 */
public final class FilesystemJobsDAO implements JobDAO {

    private static final Logger log = LoggerFactory.getLogger(FilesystemJobsDAO.class);


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
            return listDirectories(jobsDirectory)
                    .filter(this::containsARequestJsonFile)
                    .filter(this::doesNotBeginWithDot)
                    .map(this::loadJobDetails);
        }
    }

    private boolean containsARequestJsonFile(File jobDir) {
        return jobDir.toPath().resolve(JOB_DIR_JOB_DETAILS_FILENAME).toFile().exists();
    }

    private boolean doesNotBeginWithDot(File jobDir) {
        return !jobDir.getName().startsWith(".");
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

        final AtomicReference<FileOutputStream> outputStream =
                new AtomicReference<>(null);
        return o.subscribe(
                bytes -> {
                    if (outputStream.get() == null)
                        outputStream.set(new FileOutputStream(stdoutPath.toFile()));
                    outputStream.get().write(bytes);
                },
                error -> {
                    if (outputStream.get() != null)
                        outputStream.get().close();
                },
                () -> {
                    if (outputStream.get() != null)
                        outputStream.get().close();
                });
    }

    @Override
    public Disposable appendStderr(JobId jobId, Observable<byte[]> stderr) {
        return persistObservableToJobFile(jobId, JOB_DIR_STDERR_FILENAME, stderr);
    }

    @Override
    public PersistedJob persist(ValidJobRequest validJobRequest) {
        final JobId jobId = generateUniqueJobId();

        final PersistedJob persistedJob =
                PersistedJob.createFromValidRequest(validJobRequest, jobId);

        createNewJobDirectory(persistedJob);

        return persistedJob;
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

    private void createNewJobDirectory(PersistedJob persistedJob) {
        final JobId id = persistedJob.getId();
        try {
            final Path jobDir = jobsDirectory.resolve(id.toString());
            createDirectory(jobDir);
            log.debug(id + ": created job dir: " + jobDir);

            final Path jobSpecPath = jobDir.resolve(JOB_DIR_JOB_SPEC_FILENAME);
            writeJSON(jobSpecPath, persistedJob.getSpec());
            log.debug(id + ": written spec file: " + jobSpecPath);

            final Path jobDetailsPath = jobDir.resolve(JOB_DIR_JOB_DETAILS_FILENAME);
            writeJSON(jobDetailsPath, fromPersistedJob(persistedJob));
            log.debug(id + ": written job details: " + jobDetailsPath);

            final Path jobInputsPath = jobDir.resolve(JOB_DIR_JOB_INPUTS_FILENAME);
            writeJSON(jobInputsPath, persistedJob.getInputs());
            log.debug(id + ": written job inputs: " + jobInputsPath);
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

            if (maybeJobDetailsPath.isPresent())
                addJobDetails(newStatus, statusMessage, maybeJobDetailsPath.get());
            else
                throw new RuntimeException(jobId + ": cannot add new status: " + JOB_DIR_JOB_DETAILS_FILENAME + " does not exist");
        }
    }

    private void addJobDetails(JobStatus newStatus, String statusMessage, Path jobDetailsPath) {
        try {
            final JobDetails jobDetails = readJSON(jobDetailsPath, JobDetails.class);
            final JobTimestamp newTimestamp = JobTimestamp.now(newStatus, statusMessage);
            final JobDetails updatedJobDetails = jobDetails.withStatusChangeTimestamp(newTimestamp);
            writeJSON(jobDetailsPath, updatedJobDetails);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void persistOutput(JobId jobId, JobOutput jobOutput) {
        final Optional<Path> maybeJobDir = resolveJobDir(jobId);

        if (!maybeJobDir.isPresent())
            throw new RuntimeException(jobOutput.getId() + ": cannot be persisted to job " + jobId + ": job dir does not exist");

        final Path outputsDir = maybeJobDir.get().resolve(JOB_DIR_OUTPUTS_DIRNAME);
        createIfDoesNotExist(outputsDir);

        final Path outputPath = outputsDir.resolve(jobOutput.getId().toString());
        writeJobOutputToDisk(jobOutput, outputPath);

        final Optional<Path> maybeJobOutputsFile =
                resolveJobFile(jobId, JOB_DIR_OUTPUTS_FILENAME);

        try {
            final List<JobOutputDetails> existingJobOutputMetadata =
                    maybeJobOutputsFile.isPresent() ?
                            loadJSON(maybeJobOutputsFile.get(),  new TypeReference<List<JobOutputDetails>>(){}) :
                            new ArrayList<>();

            final JobOutputDetails jobOutputDetails = new JobOutputDetails(
                    jobOutput.getId(),
                    jobOutput.getData().getSizeOf(),
                    Optional.of(jobOutput.getData().getMimeType()),
                    jobOutput.getName(),
                    jobOutput.getDescription(),
                    jobOutput.getMetadata());

            existingJobOutputMetadata.add(jobOutputDetails);
            writeJSON(resolveJobDir(jobId).get().resolve(JOB_DIR_OUTPUTS_FILENAME), existingJobOutputMetadata);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void remove(JobId jobId) {
        resolveJobDir(jobId).ifPresent(dir -> {
            try {
                FileUtils.deleteDirectory(dir.toFile());
            } catch (IOException ex) {
                log.warn("{}: cannot delete job dir {}: {}", jobId, dir, ex.getMessage());
            }
        });
    }

    private void createIfDoesNotExist(Path p) {
        if (!p.toFile().exists()) {
            try {
                Files.createDirectory(p);
            } catch (IOException ex) {
                throw new RuntimeException(p + ": cannot be created: " + ex);
            }
        }
    }

    private void writeJobOutputToDisk(JobOutput jobOutput, Path outputPath) {
        try {
            IOUtils.copyLarge(jobOutput.getData().getData(), new FileOutputStream(outputPath.toFile(), false));
            jobOutput.getData().getData().close();
        } catch (IOException ex) {
            throw new RuntimeException(outputPath + ": cannot write: " + ex);
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
        return resolveJobDir(jobId).map(Path::toFile).flatMap(this::loadJobSpec);
    }

    private Optional<JobSpec> loadJobSpec(File jobDir) {
        final Path jobDetailsPath = jobDir.toPath().resolve(JOB_DIR_JOB_SPEC_FILENAME);

        if (jobDetailsPath.toFile().exists()) {
            try {
                return Optional.of(loadJSON(jobDetailsPath, JobSpec.class));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return Optional.empty();
        }
    }



    @Override
    public boolean hasStdout(JobId jobId) {
        return resolveJobFile(jobId, JOB_DIR_STDOUT_FILENAME).isPresent();
    }

    @Override
    public Optional<BinaryData> getStdout(JobId jobId) {
        return resolveJobFile(jobId, JOB_DIR_STDOUT_FILENAME).map(Helpers::streamBinaryData);
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
    public boolean hasOutput(JobId jobId, JobOutputId outputId) {
        return tryResolveOutput(jobId, outputId.toString()).isPresent();
    }

    private Optional<Path> tryResolveOutput(JobId jobId, String outputId) {
        return resolveJobDir(jobId).flatMap(p -> tryResolve(p, JOB_DIR_OUTPUTS_DIRNAME, outputId));
    }

    @Override
    public Optional<BinaryData> getOutput(JobId jobId, JobOutputId outputId) {
        return resolveJobFile(jobId, JOB_DIR_OUTPUTS_FILENAME)
                .map(this::loadJobOutputsMetadataFile)
                .flatMap(jobOutputs -> jobOutputs.stream().filter(output -> output.getId().equals(outputId)).findFirst())
                .flatMap(jobOutputMetadata -> tryLoadJobOutputData(jobId, jobOutputMetadata));
    }

    private List<JobOutputDetails> loadJobOutputsMetadataFile(Path p) {
        try {
            return loadJSON(p, new TypeReference<List<JobOutputDetails>>(){});
        } catch (IOException ex) {
            throw new RuntimeException(p.toString() + ": " + JOB_DIR_OUTPUTS_FILENAME + ": cannot parse as a job outputs metadata file");
        }
    }

    private Optional<BinaryData> tryLoadJobOutputData(JobId jobId, JobOutputDetails metadata) {
        return tryResolveOutput(jobId, metadata.getId().toString())
                .map(Helpers::streamBinaryData)
                .map(binaryData -> {
                    final String mimeType = metadata.getMimeType().orElse(binaryData.getMimeType());
                    return binaryData.withMimeType(mimeType);
                });
    }

    @Override
    public List<JobOutputDetails> getJobOutputs(JobId jobId) {
        return resolveJobFile(jobId, JOB_DIR_OUTPUTS_FILENAME)
                .map(this::loadJobOutputsMetadataFile)
                .orElse(emptyList());
    }

    @Override
    public boolean hasJobInputs(JobId jobId) {
        return resolveJobFile(jobId, JOB_DIR_JOB_INPUTS_FILENAME).isPresent();
    }

    @Override
    public Optional<Map<JobExpectedInputId, JsonNode>> getJobInputs(JobId jobId) {
        return resolveJobFile(jobId, JOB_DIR_JOB_INPUTS_FILENAME)
                .map(inputsFile -> {
                    try {
                        return loadJSON(inputsFile, new TypeReference<Map<JobExpectedInputId, JsonNode>>() {});
                    } catch (IOException ex) {
                        throw new RuntimeException(inputsFile + ": cannot deserialize: " + ex);
                    }
                });
    }

    @Override
    public Map<String, HealthCheck> getHealthChecks() {
        return singletonMap(
                FILESYSTEM_JOBS_DAO_DISK_SPACE_HEALTHCHECK,
                new DiskSpaceHealthCheck(
                        this.jobsDirectory.toFile(),
                        FILESYSTEM_JOBS_DAO_DISK_SPACE_WARNING_THRESHOLD_IN_BYTES));
    }
}
