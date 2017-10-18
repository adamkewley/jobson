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

import com.github.jobson.dao.BinaryData;
import com.github.jobson.jobinputs.JobExpectedInputId;
import com.github.jobson.jobs.jobstates.PersistedJob;
import com.github.jobson.scripting.FreeFunction;
import com.github.jobson.specs.ExecutionConfiguration;
import com.github.jobson.specs.JobDependencyConfiguration;
import com.github.jobson.specs.JobOutput;
import com.github.jobson.specs.RawTemplateString;
import com.github.jobson.utils.CancelablePromise;
import com.github.jobson.utils.SimpleCancelablePromise;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.jobson.Helpers.*;
import static com.github.jobson.jobs.JobStatus.FINISHED;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Executes a job submission as a local subprocess.
 */
public final class LocalJobExecutor implements JobExecutor {

    private static final Logger log = Logger.getLogger(LocalJobExecutor.class);


    private static String resolveArg(PersistedJob persistedJob, Path jobWorkingDir, RawTemplateString arg) {
        final Map<String, Object> environment = new HashMap<>();
        environment.put("request", persistedJob);
        environment.put("inputs", mapKeys(persistedJob.getInputs(), JobExpectedInputId::toString));

        environment.put("toJSON", new FreeFunction() {
            @Override
            public Object call(Object... args) {
                if (args.length != 1)
                    throw new RuntimeException(format("toJSON called with %s args (expects 1)", args.length));
                return toJSON(args[0]);
            }
        });

        environment.put("toFile", new FreeFunction() {
            @Override
            public Object call(Object... args) {
                if (args.length != 1) {
                    throw new RuntimeException(format("asFile called with %s args (expects 1)", args.length));
                } else if (!(args[0] instanceof String)) {
                    throw new RuntimeException(format(
                            "asFile called with %s, should be called with a string (try using toJSON?)",
                            args[0].getClass().getSimpleName()));
                } else {
                    try {
                        final String fileContent = (String)args[0];
                        final Path path = Files.createTempFile(jobWorkingDir, "request", "");
                        Files.write(path, fileContent.getBytes());
                        return path.toAbsolutePath().toString();
                    } catch (IOException ex) {
                        throw new RuntimeException(
                                format("Could not create an input file (needed in '%s').", arg), ex);
                    }
                }
            }
        });

        return arg.tryEvaluate(environment);
    }

    private static void copyJobDependency(JobDependencyConfiguration jobDependencyConfiguration, Path workingDir) {
        final Path source = Paths.get(jobDependencyConfiguration.getSource());
        final Path target = workingDir.resolve(Paths.get(jobDependencyConfiguration.getTarget()));

        try {
            if (source.toFile().isDirectory()) {
                log.debug("copy dependency: " + source.toString() + " -> " + target.toString());
                FileUtils.copyDirectory(source.toFile(), target.toFile());
            } else {
                log.debug("copy dependency: " + source.toString() + " -> " + target.toString());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            log.error(source.toString() + ": cannot copy: " + ex.toString());
            throw new RuntimeException(ex);
        }
    }


    private final Path workingDirs;
    private final int delayBeforeForciblyKillingJobs;



    public LocalJobExecutor(Path workingDirs, int delayBeforeForciblyKillingJobs) throws FileNotFoundException {
        requireNonNull(workingDirs);
        if (!workingDirs.toFile().exists())
            throw new FileNotFoundException(workingDirs + ": does not exist");
        if (delayBeforeForciblyKillingJobs < 0)
            throw new IllegalArgumentException(delayBeforeForciblyKillingJobs + ": delay before killing jobs must be positive");

        this.workingDirs = workingDirs;
        this.delayBeforeForciblyKillingJobs = delayBeforeForciblyKillingJobs;
    }



    @Override
    public CancelablePromise<JobExecutionResult> execute(PersistedJob req, JobEventListeners jobEventListeners) {

        final ExecutionConfiguration executionConfiguration = req.getSpec().getExecution();

        try {
            final Path workingDir = workingDirs.resolve(req.getId().toString());
            Files.createDirectory(workingDir);
            log.debug(req.getId() + ": created working directory: " + workingDir.toString());

            executionConfiguration.getDependencies()
                    .ifPresent(deps -> deps.forEach(dep -> copyJobDependency(dep, workingDir)));

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

            log.info(req.getId() + ": launched: " + String.join(" ", argList));

            final SimpleCancelablePromise<JobExecutionResult> ret = new SimpleCancelablePromise<>();
            ret.onCancel(() -> abort(runningProcess));

            attachTo(
                    runningProcess,
                    jobEventListeners.getOnStdoutListener(),
                    jobEventListeners.getOnStderrListener(),
                    exitCode -> {
                        final JobStatus exitStatus = JobStatus.fromExitCode(exitCode);
                        if (exitStatus == FINISHED) {
                            final Map<String, JobOutput> expectedOutputs = req.getSpec().getOutputs();
                            final Map<String, BinaryData> outputs = getJobOutputs(workingDir, expectedOutputs);
                            ret.complete(new JobExecutionResult(exitStatus, outputs));
                        } else ret.complete(new JobExecutionResult(exitStatus));
                    });

            return ret;

        } catch (Exception ex) {
            log.error(req.getId() + ": cannot start: " + ex.toString());
            throw new RuntimeException(ex);
        }
    }

    private Map<String, BinaryData> getJobOutputs(Path workingDir, Map<String, JobOutput> expectedOutputs) {
        final Map<String, BinaryData> ret = new HashMap<>();

        for (Map.Entry<String, JobOutput> expectedOutput : expectedOutputs.entrySet()) {
            final Path expectedOutputFile =
                    workingDir.resolve(expectedOutput.getValue().getPath());

            if (expectedOutputFile.toFile().exists()) {
                ret.put(expectedOutput.getKey(), streamBinaryData(expectedOutputFile));
            }
        }

        return ret;
    }

    private void abort(Process process) {
        log.debug("Aborting process: " + process);
        process.destroy();
        try {
            final boolean terminated = process.waitFor(delayBeforeForciblyKillingJobs, TimeUnit.SECONDS);
            if (!terminated) {
                log.warn(process + " did not abort within " + delayBeforeForciblyKillingJobs + " seconds, aborting forcibly (SIGKILL)");
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            log.error("Abortion interrupted while waiting on process (this shouldn't happen)");
        }
    }
}
