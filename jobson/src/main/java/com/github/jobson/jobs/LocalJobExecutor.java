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

package com.github.jobson.jobs;

import com.github.jobson.Constants;
import com.github.jobson.Helpers;
import com.github.jobson.config.RemoveAfterExecutionConfig;
import com.github.jobson.jobinputs.JobExpectedInputId;
import com.github.jobson.jobs.jobstates.PersistedJob;
import com.github.jobson.scripting.functions.*;
import com.github.jobson.specs.*;
import com.github.jobson.utils.BinaryData;
import com.github.jobson.utils.CancelablePromise;
import com.github.jobson.utils.SimpleCancelablePromise;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.jobson.Helpers.*;
import static com.github.jobson.jobs.JobStatus.FINISHED;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Executes a job submission as a local subprocess.
 */
public final class LocalJobExecutor implements JobExecutor {

    private static final Logger log = Logger.getLogger(LocalJobExecutor.class);


    public static String resolveArg(PersistedJob persistedJob, Path jobWorkingDir, RawTemplateString arg) {
        final Map<String, Object> environment = new HashMap<>();

        environment.put("toJSON", new ToJSONFunction());
        environment.put("toFile", new ToFileFunction(jobWorkingDir));
        environment.put("join", new JoinFunction());
        environment.put("toString", new ToStringFunction());
        environment.put("request", persistedJob);
        environment.put("inputs", mapKeys(persistedJob.getInputs(), JobExpectedInputId::toString));
        environment.put("outputDir", jobWorkingDir.toString());
        environment.put("toDir", new ToDirFunction(jobWorkingDir));

        return arg.tryEvaluate(environment);
    }

    private static void handleJobDependency(PersistedJob persistedJob, Path workingDir, JobDependencyConfiguration jobDependencyConfiguration) {
        final String resolvedSourceStr = resolveArg(persistedJob, workingDir, jobDependencyConfiguration.getSource());
        final Path source = Paths.get(resolvedSourceStr);

        final String resolvedTargetStr = resolveArg(persistedJob, workingDir, jobDependencyConfiguration.getTarget());
        final Path target = workingDir.resolve(resolvedTargetStr);

        if (jobDependencyConfiguration.isSoftLink()) {
            softLinkJobDependency(source, target);
        } else {
            copyJobDependency(source, target);
        }
    }

    private static void softLinkJobDependency(Path source, Path destination) {
        log.debug("softlink dependency: " + source.toString() + " -> " + destination.toString());
        try {
            Files.createSymbolicLink(destination, source);
        } catch (UnsupportedOperationException | IOException ex) {
            log.error(source.toString() + ": cannot create soft link: " + ex.toString());
        }
    }

    private static void copyJobDependency(Path source, Path destination) {
        log.debug("copy dependency: " + source.toString() + " -> " + destination.toString());
        try {
            Helpers.copyPath(source, destination);
        } catch (IOException ex) {
            log.error(source.toString() + ": cannot copy: " + ex.toString());
            throw new RuntimeException(ex);
        }
    }


    private final Path workingDirs;
    private final long delayBeforeForciblyKillingJobs;
    private final boolean deleteWdAfterExecution;



    public LocalJobExecutor(Path workingDirs, long delayBeforeForciblyKillingJobs) throws FileNotFoundException {
        this(workingDirs, delayBeforeForciblyKillingJobs, new RemoveAfterExecutionConfig());
    }

    public LocalJobExecutor(Path workingDirs,
                            long delayBeforeForciblyKillingJobs,
                            RemoveAfterExecutionConfig wdRemovalConfig) throws FileNotFoundException {
        requireNonNull(workingDirs);
        if (!workingDirs.toFile().exists())
            throw new FileNotFoundException(workingDirs + ": does not exist");
        if (delayBeforeForciblyKillingJobs < 0)
            throw new IllegalArgumentException(delayBeforeForciblyKillingJobs + ": delay before killing jobs must be positive");

        this.workingDirs = workingDirs.toAbsolutePath();
        this.delayBeforeForciblyKillingJobs = delayBeforeForciblyKillingJobs;
        this.deleteWdAfterExecution = wdRemovalConfig.isEnabled();
    }



    @Override
    public CancelablePromise<JobExecutionResult> execute(PersistedJob req, JobEventListeners jobEventListeners) {
        final ExecutionConfiguration executionConfiguration = req.getSpec().getExecution();

        try {
            final Path workingDir = workingDirs.resolve(req.getId().toString());
            Files.createDirectory(workingDir);
            log.debug(req.getId() + ": created working directory: " + workingDir.toString());

            executionConfiguration.getDependencies()
                    .ifPresent(deps -> deps.forEach(dep -> handleJobDependency(req, workingDir, dep)));

            final String application = executionConfiguration.getApplication();
            final List<String> argList = new ArrayList<>();
            argList.add(application);

            log.debug(req.getId() + ": resolving args");

            executionConfiguration.getArguments()
                    .ifPresent(args -> args.stream()
                            .map(arg -> resolveArg(req, workingDir, arg))
                            .forEach(argList::add));

            final ProcessBuilder processBuilder = new ProcessBuilder(argList);

            processBuilder.directory(workingDir.toFile());

            log.debug(req.getId() + ": launch subprocess: " + String.join(" ", argList));

            final Process runningProcess = processBuilder.start();

            // close process's stdin stream. If this isn't done, the
            // child process will block if it tries to read from stdin
            // (because it's connected to jobson's stdin, which isn't
            // being used)
            //
            // see https://github.com/adamkewley/jobson/issues/67 for
            // a breakdown of the kinds of problems this can create
            runningProcess.getOutputStream().close();

            log.info(req.getId() + ": launched: " + String.join(" ", argList));

            final SimpleCancelablePromise<JobExecutionResult> ret = new SimpleCancelablePromise<>();
            ret.onCancel(() -> abort(runningProcess));

            attachTo(
                    runningProcess,
                    jobEventListeners.getOnStdoutListener(),
                    jobEventListeners.getOnStderrListener(),
                    exitCode -> onProcessExit(req, workingDir, ret, exitCode));

            return ret;

        } catch (Exception ex) {
            log.error(req.getId() + ": cannot start: " + ex.toString());
            throw new RuntimeException(ex);
        }
    }

    private void onProcessExit(
            PersistedJob req,
            Path workingDir,
            SimpleCancelablePromise<JobExecutionResult> promise,
            int exitCode) {

        final JobStatus exitStatus = JobStatus.fromExitCode(exitCode);

        final JobExecutionResult jobExecutionResult;
        if (exitStatus == FINISHED) {
            final List<JobOutputResult> outputs = tryResolveJobOutputs(req, workingDir, req.getSpec().getExpectedOutputs());
            jobExecutionResult = new JobExecutionResult(exitStatus, outputs);
        } else {
            jobExecutionResult = new JobExecutionResult(exitStatus);
        }

        promise.complete(jobExecutionResult);

        if (this.deleteWdAfterExecution) {
            try {
                FileUtils.deleteDirectory(workingDir.toFile());
            } catch (IOException e) {
                log.warn(format("Tried to remove a working directory, %s, but couldn't: %s", workingDir, e.getMessage()));
            }
        }
    }

    private List<JobOutputResult> tryResolveJobOutputs(
            PersistedJob req,
            Path workingDir,
            List<JobExpectedOutput> expectedOutputs) {

        return expectedOutputs
                .stream()
                .map(e -> {
                    final JobOutputId jobOutputId = new JobOutputId(resolveArg(req, workingDir, e.getId()));
                    return tryGetJobOutput(workingDir, req, jobOutputId, e);
                })
                .collect(Collectors.toList());
    }

    private JobOutputResult tryGetJobOutput(Path workingDir, PersistedJob job, JobOutputId outputId, JobExpectedOutput expectedOutput) {
        final Path expectedOutputFile = workingDir.resolve(resolveArg(job, workingDir, expectedOutput.getPath()));

        if (expectedOutputFile.toFile().exists()) {
            final String mimeType = establishMimeType(expectedOutput, expectedOutputFile);
            final BinaryData data = streamBinaryData(expectedOutputFile, mimeType);
            return new JobOutput(
                    outputId,
                    data,
                    expectedOutput.getName(),
                    expectedOutput.getDescription(),
                    expectedOutput.getMetadata());
        } else {
            return new MissingOutput(
                    outputId,
                    expectedOutput.isRequired(),
                    expectedOutputFile.relativize(workingDir).toString());
        }
    }

    private String establishMimeType(JobExpectedOutput jobExpectedOutput, Path p) {
        if (jobExpectedOutput.getMimeType().isPresent()) {
            return jobExpectedOutput.getMimeType().get();
        } else {
            try {
                return Helpers.getMimeType(Files.newInputStream(p), p.toString());
            } catch (IOException ex) {
                log.warn("Encountered IO error when determining an output's MIME type. Skipping MIME type detection");
                return Constants.DEFAULT_BINARY_MIME_TYPE;
            }
        }
    }

    private void abort(Process process) {
        log.debug("Aborting process: " + process);
        process.destroy();
        try {
            final boolean terminated = process.waitFor(delayBeforeForciblyKillingJobs, TimeUnit.MILLISECONDS);
            if (!terminated) {
                log.warn(process + " did not abort within " + delayBeforeForciblyKillingJobs + " seconds, aborting forcibly (SIGKILL)");
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            log.error("Abortion interrupted while waiting on process (this shouldn't happen)");
        }
    }
}
