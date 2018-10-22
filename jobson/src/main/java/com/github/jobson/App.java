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

package com.github.jobson;

import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.jobson.auth.AuthenticationBootstrap;
import com.github.jobson.commands.*;
import com.github.jobson.config.ApplicationConfig;
import com.github.jobson.dao.jobs.FilesystemJobsDAO;
import com.github.jobson.dao.jobs.JobDAO;
import com.github.jobson.dao.specs.FilesystemJobSpecDAO;
import com.github.jobson.dao.specs.JobSpecDAO;
import com.github.jobson.dao.users.FilesystemUserDAO;
import com.github.jobson.dao.users.UserDAO;
import com.github.jobson.jobs.JobExecutor;
import com.github.jobson.jobs.JobManager;
import com.github.jobson.jobs.JobStatus;
import com.github.jobson.jobs.LocalJobExecutor;
import com.github.jobson.resources.RootResource;
import com.github.jobson.resources.v1.JobResource;
import com.github.jobson.resources.v1.JobSpecResource;
import com.github.jobson.resources.v1.UserResource;
import com.github.jobson.resources.v1.V1RootResource;
import com.github.jobson.websockets.v1.JobEventSocketCreator;
import com.github.jobson.websockets.v1.StderrUpdateSocketCreator;
import com.github.jobson.websockets.v1.StdoutUpdateSocketCreator;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.http.pathmap.RegexPathSpec;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Map;

import static com.github.jobson.Constants.*;
import static com.github.jobson.Helpers.generateRandomBase36String;
import static com.github.jobson.Helpers.merge;
import static com.github.jobson.jobs.JobStatus.ABORTED;

public final class App extends Application<ApplicationConfig> {

    private static Logger log = LoggerFactory.getLogger(App.class);


    public static void main(String[] args) throws Exception {
        new App().run(args);
    }


    public void initialize(Bootstrap<ApplicationConfig> configurationBootstrap) {
        configurationBootstrap.addCommand(new NewCommand());
        configurationBootstrap.addCommand(new GenerateCommand());
        configurationBootstrap.addCommand(new UsersCommand());
        configurationBootstrap.addCommand(new ValidateCommand());
        configurationBootstrap.addCommand(new RunCommand());
    }

    public void run(ApplicationConfig applicationConfig, Environment environment) throws Exception {
        environment.jersey().register(new JsonProcessingExceptionMapper(true));
        environment.getObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, Constants.API_PRETTIFY_JSON_OUTPUT);


        final Path userFilePath = Paths.get(applicationConfig.getUsersConfiguration().getFile());

        if (!userFilePath.toFile().exists()) {
            final String error = userFilePath.toString() + ": Does not exist. A user file is REQUIRED to boot the server";
            log.error(error);
            throw new RuntimeException(error);
        }

        log.debug("User details will be read from " + userFilePath.toString());
        final UserDAO userDAO = new FilesystemUserDAO(userFilePath.toFile());



        log.debug("Enabling authentication");
        final AuthenticationBootstrap authenticationBootstrap = new AuthenticationBootstrap(environment.jersey(), userDAO);
        final AuthFilter<?, Principal> authFilter =
                applicationConfig.getAuthenticationConfiguration().createAuthFilter(authenticationBootstrap);

        environment.jersey().register(new AuthDynamicFeature(authFilter));
        environment.jersey().register(RolesAllowedDynamicFeature.class);



        final Path jobSpecsPath = Paths.get(applicationConfig.getJobSpecConfiguration().getDir());

        log.debug("Loading a JobSpecDAO backed by " + jobSpecsPath.toString());
        final JobSpecDAO jobSpecDAO = new FilesystemJobSpecDAO(jobSpecsPath);


        log.debug("Registering root API");
        environment.jersey().register(new RootResource());

        log.debug("Registering root v1 API");
        final V1RootResource v1RootResource = new V1RootResource();
        environment.jersey().register(v1RootResource);


        log.debug("Registering the job specs API");
        final JobSpecResource jobSpecResource = new JobSpecResource(jobSpecDAO, Constants.DEFAULT_PAGE_SIZE);
        environment.jersey().register(jobSpecResource);


        final Path jobsPath = Paths.get(applicationConfig.getJobDataConfiguration().getDir());

        if (!jobsPath.toFile().exists()) {
            log.debug(jobsPath + ": does not exist. Creating");
            Files.createDirectory(jobsPath);
        }



        final Path workingDirsPath = Paths.get(applicationConfig.getWorkingDirs().getDir());
        if (!workingDirsPath.toFile().exists()) {
            log.info(workingDirsPath + ": does not exist. Creating");
            Files.createDirectory(workingDirsPath);
        }

        log.debug("Creating job executor");
        final JobExecutor jobExecutor = new LocalJobExecutor(
                workingDirsPath,
                applicationConfig.getExecution().getDelayBeforeForciblyKillingJobs().toMillis(),
                applicationConfig.getWorkingDirs().getRemoveAfterExecutionConfig());

        log.debug("Creating job DAO");
        final JobDAO jobDAO = new FilesystemJobsDAO(jobsPath, () -> generateRandomBase36String(10));


        log.debug("Creating job manager");
        final JobManager jobManager = new JobManager(jobDAO, jobExecutor, applicationConfig.getExecution().getMaxConcurrentJobs());


        log.debug("Registering the jobs API");

        final JobResource jobResource = new JobResource(jobManager, jobDAO, jobSpecDAO, Constants.DEFAULT_PAGE_SIZE);
        environment.jersey().register(jobResource);



        log.debug("Registering users API");
        final UserResource userResource = new UserResource();
        environment.jersey().register(userResource);



        log.debug("Enabling websockets");
        final WebSocketUpgradeFilter wsFilter = WebSocketUpgradeFilter.configureContext(environment.getApplicationContext());
        wsFilter.getFactory().getPolicy().setIdleTimeout(WEBSOCKET_TCP_IDLE_TIMEOUT_IN_MILLISECONDS);

        log.debug("Enabling job events (multi-job) websocket endpoint");
        wsFilter.addMapping(Constants.WEBSOCKET_JOB_EVENTS_PATH, new JobEventSocketCreator(jobManager));

        log.debug("Enabling job stderr updates websocket endpoint");
        wsFilter.addMapping(
                new RegexPathSpec(WEBSOCKET_STDERR_UPDATES_PATTERN),
                new StderrUpdateSocketCreator(jobManager));

        log.debug("Enabling job stdout updates websocket endpoint");
        wsFilter.addMapping(
                new RegexPathSpec(WEBSOCKET_STDOUT_UPDATES_PATTERN),
                new StdoutUpdateSocketCreator(jobManager));



        log.debug("Aborting dangling jobs");
        JobStatus.getAbortableStatuses().stream()
                .flatMap(s -> jobDAO.getJobsWithStatus(s).stream())
                .forEach(id -> jobDAO.addNewJobStatus(id, ABORTED, "Aborted as dangling job"));



        log.debug("Enabling healthchecks");
        final Map<String, HealthCheck> healthChecks =
                merge(merge(jobDAO.getHealthChecks(), jobSpecDAO.getHealthChecks()), jobManager.getHealthChecks());
        for (Map.Entry<String, HealthCheck> healthCheckEntry : healthChecks.entrySet()) {
            environment.healthChecks().register(healthCheckEntry.getKey(), healthCheckEntry.getValue());
        }
    }
}
