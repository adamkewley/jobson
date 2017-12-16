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

package com.github.jobson.commands;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.core.JsonParseException;
import com.github.jobson.Constants;
import com.github.jobson.Helpers;
import com.github.jobson.api.v1.APIJobRequest;
import com.github.jobson.api.v1.UserId;
import com.github.jobson.config.ApplicationConfig;
import com.github.jobson.dao.jobs.FilesystemJobsDAO;
import com.github.jobson.dao.specs.FilesystemJobSpecDAO;
import com.github.jobson.jobs.JobEventListeners;
import com.github.jobson.jobs.JobExecutor;
import com.github.jobson.jobs.JobManager;
import com.github.jobson.jobs.LocalJobExecutor;
import com.github.jobson.jobs.jobstates.FinalizedJob;
import com.github.jobson.jobs.jobstates.ValidJobRequest;
import com.github.jobson.utils.EitherVisitor;
import com.github.jobson.utils.ValidationError;
import io.dropwizard.setup.Bootstrap;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.github.jobson.Helpers.commaSeparatedList;
import static com.github.jobson.Helpers.readJSON;
import static com.github.jobson.resources.v1.JobResource.validateAPIRequest;

public final class RunCommand extends DefaultedConfiguredCommand<ApplicationConfig> {

    private static final String REQUEST_FILE_ARGNAME = "REQUEST_JSON";
    private static final Logger log = LoggerFactory.getLogger(RunCommand.class.getName());


    public RunCommand() {
        super("run", "run a request locally");
    }


    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument(REQUEST_FILE_ARGNAME)
                .metavar(REQUEST_FILE_ARGNAME)
                .type(String.class)
                .nargs(1)
                .help("The request to run");
    }

    @Override
    protected void run(Bootstrap<ApplicationConfig> bootstrap, Namespace namespace, ApplicationConfig applicationConfig) throws Exception {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.OFF);

        final Path requestPath = Paths.get(namespace.getList(REQUEST_FILE_ARGNAME).get(0).toString());

        if (!Files.exists(requestPath)) {
            System.err.println(requestPath.toString() + ": No such file");
            System.exit(1);
        }

        final byte[] requestBytes;
        try {
            requestBytes = Files.readAllBytes(requestPath);
        } catch (IOException ex) {
            System.err.println(requestPath + ": cannot read: " + ex.getMessage());
            System.exit(1);
            return;
        }

        final String requestJson = new String(requestBytes);

        final APIJobRequest jobSubmissionRequest;
        try {
            jobSubmissionRequest = readJSON(requestJson, APIJobRequest.class);
        } catch (JsonParseException ex) {
            System.err.println("Could not parse json. Message: " + ex.getMessage());
            System.exit(1);
            return;
        }

        log.debug("Job submission request parsed successfully.");

        final Path tmpDir = Files.createTempDirectory(RunCommand.class.getSimpleName());

        log.debug("Created temporary directory: " + tmpDir);

        final FilesystemJobSpecDAO filesystemJobSpecDAO =
                new FilesystemJobSpecDAO(Paths.get(applicationConfig.getJobSpecConfiguration().getDir()));

        log.info("Creating temporary directory for job working dirs");
        final Path jobWorkingDirs = Files.createTempDirectory("wds");
        final JobExecutor jobExecutor = new LocalJobExecutor(jobWorkingDirs, Constants.DELAY_BEFORE_FORCIBLY_KILLING_JOBS_IN_MILLISECONDS);

        final FilesystemJobsDAO filesystemJobsDAO = new FilesystemJobsDAO(tmpDir, () -> Helpers.generateRandomBase36String(10));

        final JobManager jobManager = new JobManager(filesystemJobsDAO, jobExecutor, Constants.MAX_CONCURRENT_JOBS);

        final JobEventListeners listeners = createJobEventListeners();

        final UserId userId = new UserId("jobson-run-command");
        log.debug("Submitting job request");

        validateAPIRequest(jobSubmissionRequest, filesystemJobSpecDAO, userId)
                .visit(createResultVisitor(jobManager, listeners));
    }

    private JobEventListeners createJobEventListeners() {
        return JobEventListeners.create(
                new Observer<byte[]>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable disposable) {}

                    @Override
                    public void onNext(@NonNull byte[] bytes) {
                        try {
                            System.out.write(bytes);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {
                        System.err.println("Error in stdout: " + throwable.toString());
                        System.exit(1);
                    }

                    @Override
                    public void onComplete() {
                        System.exit(0);
                    }
                },
                new Observer<byte[]>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable disposable) {}

                    @Override
                    public void onNext(@NonNull byte[] bytes) {
                        try {
                            System.out.write(bytes);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {
                        System.err.println("Error in stderr: " + throwable.toString());
                        System.exit(1);
                    }

                    @Override
                    public void onComplete() {
                        System.exit(0);
                    }
                });
    }

    private EitherVisitor<ValidJobRequest, List<ValidationError>> createResultVisitor(
            JobManager jobManager,
            JobEventListeners listeners) {

        return new EitherVisitor<ValidJobRequest, List<ValidationError>>() {
            @Override
            public void whenLeft(ValidJobRequest left) {
                try {
                    final FinalizedJob f = jobManager.submit(left, listeners).getRight().get();
                    System.exit(f.getFinalStatus().toExitCode());
                } catch (Exception ex) {
                    System.err.println("Error encountered: " + ex.toString());
                    System.exit(1);
                }
            }

            @Override
            public void whenRight(List<ValidationError> right) {
                System.err.println("Invalid request: " + commaSeparatedList(right));
                System.exit(1);
            }
        };
    }
}
