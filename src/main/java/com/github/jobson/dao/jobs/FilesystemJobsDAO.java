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
import com.github.jobson.api.v1.*;
import com.github.jobson.dao.BinaryData;
import com.github.jobson.dao.IdGenerator;
import com.github.jobson.jobs.states.PersistedJobRequest;
import com.github.jobson.jobs.states.ValidJobRequest;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static com.github.jobson.Helpers.tryResolve;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
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
            throw new FileNotFoundException(jobsDirectory.toString() + ": Does not exist");

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
        return Helpers.tryResolve(jobsDirectory, jobId.toString(), filename);

    }

    @Override
    public List<JobSummary> getJobSummaries(int pageSize, int page) {
        return getJobSummaries(pageSize, page, "");
    }

    @Override
    public List<JobSummary> getJobSummaries(int pageSize, int pageNumber, String query) {

        if (pageSize < 0) throw new IllegalArgumentException("pageSize is below 0");
        if (pageNumber < 0) throw new IllegalArgumentException("page is below 0");
        if (query == null) throw new IllegalArgumentException("query string is null");

        final Predicate<JobDetailsResponse> resultFilter = jobDetails -> {
            final String allFields =
                    jobDetails.getName() + jobDetails.getOwner().getId().toString() + jobDetails.getId();
            return allFields.toLowerCase().contains(query.toLowerCase());
        };

        synchronized (fsLock) {
            return Helpers.listDirectories(jobsDirectory)
                    .map(this::loadJobDetails)
                    .filter(resultFilter)
                    .sorted(FilesystemJobsDAO::byFirstStatusDate)
                    .skip(pageSize * pageNumber)
                    .limit(pageSize)
                    .map(JobDetailsResponse::toSummary)
                    .collect(toList());
        }
    }

    private JobDetailsResponse loadJobDetails(File jobDir) {
        final Path metadataPath = jobDir.toPath().resolve(Constants.METADATA_FILENAME);

        try {
            return Helpers.loadJSON(metadataPath, JobDetailsResponse.class);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static int byFirstStatusDate(JobDetailsResponse a, JobDetailsResponse b) {
        return b.firstStatusDate().compareTo(a.firstStatusDate());
    }

    @Override
    public Disposable appendStdout(JobId jobId, Observable<byte[]> stdout) {
        return persistObservableToJobFile(jobId, Constants.STDOUT_FILENAME, stdout);
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
        return persistObservableToJobFile(jobId, Constants.STDERR_FILENAME, stderr);
    }

    @Override
    public PersistedJobRequest persist(ValidJobRequest validJobRequest) {
        final JobId jobId = generateUniqueJobId();

        final PersistedJobRequest persistedJobRequest = new PersistedJobRequest(
                jobId,
                validJobRequest.getOwner(),
                validJobRequest.getName(),
                validJobRequest.getInputs(),
                validJobRequest.getSpec());

        createNewJobDirectory(persistedJobRequest);

        return persistedJobRequest;
    }

    private JobId generateUniqueJobId() {
        for(int i = 0; i < Constants.MAX_JOB_ID_GENERATION_ATTEMPTS; i++) {
            final String id = idGenerator.generate();
            if (!Files.exists(this.jobsDirectory.resolve(id))) {
                return new JobId(id);
            }
        }

        final String errorMsg = String.format("Could not generate unique job ID after %s attempts", Constants.MAX_JOB_ID_GENERATION_ATTEMPTS);
        log.error(errorMsg);
        throw new RuntimeException(errorMsg);
    }

    private void createNewJobDirectory(PersistedJobRequest persistedJobRequest) {
        final JobId id = persistedJobRequest.getId();
        try {
            final Path jobDir = jobsDirectory.resolve(id.toString());
            Files.createDirectory(jobDir);
            log.debug(id + ": created job dir: " + jobDir.toString());

            final Path jobRequestPath = jobDir.resolve(Constants.JOB_REQUEST_FILENAME);
            Helpers.writeJSON(jobRequestPath, persistedJobRequest);
            log.debug(id + ": written job request: " + jobRequestPath.toString());


            final Path jobMetadataPath = jobDir.resolve(Constants.METADATA_FILENAME);
            final JobDetailsResponse jobDetailsResponse = new JobDetailsResponse(
                    id,
                    persistedJobRequest.getName(),
                    new UserSummary(persistedJobRequest.getOwner()),
                    persistedJobRequest.getSpec().toSummary(),
                    singletonList(JobStatusChangeTimestamp.now(JobStatus.RUNNING, "JobCallbacks directory created")),
                    new HashMap<>());

            Helpers.writeJSON(jobMetadataPath, jobDetailsResponse);
            log.debug(id + ": written job metadata: " + jobMetadataPath.toString());


            final Path jobSchemaPath = jobDir.resolve(Constants.JOB_SCHEMA_FILENAME);
            Helpers.writeJSON(jobSchemaPath, persistedJobRequest.getSpec());
            log.debug(id + ": written job spec: " + jobSchemaPath.toString());
        } catch (IOException ex) {
            log.error(id + ": could not setup job directory: " + ex.toString());
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addNewJobStatus(JobId jobId, JobStatus newStatus, String statusMessage) {
        if (!resolveJobDir(jobId).isPresent())
            throw new RuntimeException(jobId + ": cannot add status: job does not exist");

        synchronized (fsLock) {
            final Optional<Path> maybeMetadataPath = resolveJobFile(jobId, Constants.METADATA_FILENAME);

            if (maybeMetadataPath.isPresent()) {
                try {
                    final Path metadataPath = maybeMetadataPath.get();
                    final JobDetailsResponse loadedMetadata = Helpers.readJSON(metadataPath, JobDetailsResponse.class);
                    final JobStatusChangeTimestamp newTimestamp = JobStatusChangeTimestamp.now(newStatus, statusMessage);
                    final JobDetailsResponse updatedMetadata = loadedMetadata.withStatusChangeTimestamp(newTimestamp);
                    Helpers.writeJSON(metadataPath, updatedMetadata);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                throw new RuntimeException(jobId + ": cannot add new status: " + Constants.METADATA_FILENAME + " does not exist");
            }
        }
    }

    @Override
    public Optional<JobDetailsResponse> getJobDetailsById(JobId jobId) {
        synchronized (fsLock) {
            return resolveJobFile(jobId, Constants.METADATA_FILENAME)
                    .flatMap(metadataPath -> {
                        try {
                            return Optional.of(Helpers.readJSON(metadataPath, JobDetailsResponse.class));
                        } catch (IOException ex) {
                            log.error(jobId + ": Cannot parse: " + ex.toString());
                            return Optional.empty();
                        }
                    });
        }
    }

    @Override
    public boolean hasStderr(JobId jobId) {
        return resolveJobFile(jobId, Constants.STDERR_FILENAME).isPresent();
    }

    @Override
    public Optional<BinaryData> getStderr(JobId jobId) {
        return resolveJobFile(jobId, Constants.STDERR_FILENAME).map(Helpers::streamBinaryData);
    }

    @Override
    public Set<JobId> getJobsWithStatus(JobStatus status) {
        return Helpers.listDirectories(jobsDirectory)
                .map(jobDir -> {
                    final Path metadata = jobDir.toPath().resolve(Constants.METADATA_FILENAME);
                    Optional<JobId> ret;

                    if (metadata.toFile().exists()) {
                        try {
                            final JobDetailsResponse data = Helpers.readJSON(metadata, JobDetailsResponse.class);
                            final JobStatus loadedStatus = data.latestStatus();
                            if (loadedStatus.equals(status)) {
                                ret = Optional.of(data.getId());
                            } else ret = Optional.empty();
                        } catch (IOException e) {
                            log.error(metadata + ": is not a valid metadata file. Skipping.");
                            ret = empty();
                        }
                    } else ret = empty();

                    return ret;
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toSet());
    }

    @Override
    public boolean hasStdout(JobId jobId) {
        return resolveJobFile(jobId, Constants.STDOUT_FILENAME).isPresent();
    }

    @Override
    public Optional<BinaryData> getStdout(JobId jobId) {
        return resolveJobFile(jobId, Constants.STDOUT_FILENAME).map(Helpers::streamBinaryData);
    }
}
