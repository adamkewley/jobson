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

package com.github.jobson.jobs.execution;

import com.github.jobson.jobs.management.JobEventListeners;
import com.github.jobson.jobs.states.PersistedJobRequest;
import com.github.jobson.specs.ExecutionConfiguration;
import com.github.jobson.specs.JobDependencyConfiguration;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.jobson.Helpers.attachTo;
import static com.github.jobson.Helpers.writeJSON;
import static java.util.Objects.requireNonNull;

/**
 * Executes a job submission as a local subprocess.
 */
public final class LocalJobExecutor implements JobExecutor {

    private static final Logger log = Logger.getLogger(LocalJobExecutor.class);



    private static String resolveArg(PersistedJobRequest persistedJobRequest, Path jobWorkingDir, String arg) {
        if (arg.equals("$request")) {
            try {
                final Path path = Files.createTempFile(jobWorkingDir, "request", "");
                writeJSON(path, persistedJobRequest);
                return path.toAbsolutePath().toString();
            } catch (IOException ex) {
                log.error("Cannot resolve argument: " + ex.toString());
                throw new RuntimeException(ex);
            }
        } else return arg;
    }

    private static void copyJobDependency(JobDependencyConfiguration jobDependencyConfiguration, Path workingDir) {
        final Path source = Paths.get(jobDependencyConfiguration.getSource());
        final Path target = workingDir.resolve(Paths.get(jobDependencyConfiguration.getTarget()));

        try {
            if (source.toFile().isDirectory()) {
                log.info("copy dependency: " + source.toString() + " -> " + target.toString());
                FileUtils.copyDirectory(source.toFile(), target.toFile());
            } else {
                log.info("copy dependency: " + source.toString() + " -> " + target.toString());
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
    public CancelablePromise<JobExecutionResult> execute(PersistedJobRequest req, JobEventListeners jobEventListeners) {

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
                    exitCode -> ret.complete(JobExecutionResult.fromExitCode(exitCode)));

            return ret;

        } catch (Exception ex) {
            log.error(req.getId() + ": cannot start: " + ex.toString());
            throw new RuntimeException(ex);
        }
    }

    private void abort(Process process) {
        process.destroy();
        try {
            final boolean terminated = process.waitFor(delayBeforeForciblyKillingJobs, TimeUnit.SECONDS);
            if (!terminated) process.destroyForcibly();
        } catch (InterruptedException e) {
            log.error("Abortion interrupted while waiting on process (this shouldn't happen)");
        }
    }
}
