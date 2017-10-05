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

package com.github.jobson.resources.v1;

import com.github.jobson.Helpers;
import com.github.jobson.api.v1.*;
import com.github.jobson.dao.BinaryData;
import com.github.jobson.dao.jobs.ReadonlyJobDAO;
import com.github.jobson.dao.specs.JobSpecConfigurationDAO;
import com.github.jobson.jobs.management.JobManagerActions;
import com.github.jobson.jobs.states.ValidJobRequest;
import com.github.jobson.specs.JobSpec;
import com.github.jobson.utils.Either;
import com.github.jobson.utils.EitherVisitorT;
import com.github.jobson.utils.ValidationError;
import io.swagger.annotations.*;
import org.apache.commons.io.IOUtils;

import javax.annotation.security.PermitAll;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Api(description = "Operations related to jobs")
@Path(JobResource.PATH)
@Produces("application/json")
public final class JobResource {

    public static final String PATH = "/v1/jobs";

    private static final int MAX_PAGE_SIZE = 20;



    private final JobManagerActions jobManagerActions;
    private final JobSpecConfigurationDAO jobSpecConfigurationDAO;
    private final int defaultPageSize;
    private final ReadonlyJobDAO jobDAO;


    public JobResource(
            JobManagerActions jobManagerActions,
            ReadonlyJobDAO jobDAO,
            JobSpecConfigurationDAO jobSpecConfigurationDAO,
            int defaultPageSize) throws RuntimeException {

        requireNonNull(jobManagerActions);
        requireNonNull(jobDAO);
        requireNonNull(jobSpecConfigurationDAO);
        if (defaultPageSize < 0) throw new RuntimeException("Default page size cannot be negative");

        this.jobManagerActions = jobManagerActions;
        this.jobDAO = jobDAO;
        this.jobSpecConfigurationDAO = jobSpecConfigurationDAO;
        this.defaultPageSize = defaultPageSize;
    }



    @GET
    @ApiOperation(
            value = "Get summaries of jobs managed by the system.",
            code = 200,
            notes = "Get summaries of *some* of the jobs managed by the system. The " +
                    "response does not necessarily contain summaries for *all* the jobs managed " +
                    "by the system. This is because pagination and client permissions may hide entries. ")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Job summaries returned", response = JobSummariesResponse.class)
    })
    @PermitAll
    public JobSummariesResponse fetchJobSummaries(
            @Context
                    SecurityContext context,
            @ApiParam(value = "The page number (0-indexed)")
            @QueryParam("page")
                    Optional<Integer> page,
            @ApiParam(value = "The number of entries a response page should contain. Max page size is " + MAX_PAGE_SIZE)
            @QueryParam("page-size")
                    Optional<Integer> pageSize,
            @ApiParam(value = "Client query string")
            @QueryParam("query")
                    Optional<String> query) {

        final int pageRequested = page.isPresent() ? page.get() : 0;
        final int pageSizeRequested = pageSize.isPresent() ? pageSize.get() : defaultPageSize;

        if (pageRequested < 0) throw new WebApplicationException("Page specified is negative - only positive numbers are allowed", 400);
        if (pageSizeRequested < 0) throw new WebApplicationException("Page size specified is negative - only positive numbers are allowed", 400);

        try {
            final List<JobSummary> jobSummaries =
                    query.isPresent() ?
                            jobDAO.getJobSummaries(pageSizeRequested, pageRequested, query.get()) :
                            jobDAO.getJobSummaries(pageSizeRequested, pageRequested);

            for (JobSummary jobSummary: jobSummaries) {
                final Map<String, RESTLink> restLinks = getRestLinksFromJobSummary(jobSummary);
                jobSummary.getLinks().putAll(restLinks);
            }

            return new JobSummariesResponse(jobSummaries, new HashMap<>());
        } catch (IOException ex) {
            throw new WebApplicationException(ex, 500);
        }
    }

    private Map<String, RESTLink> getRestLinksFromJobSummary(JobSummary jobSummary) throws IOException {
        try {
            final HashMap<String, RESTLink> ret = new HashMap<>();

            final URI jobDetailsURI = new URI(JobResource.PATH + "/" + jobSummary.getId().toString());
            ret.put("details", new RESTLink(jobDetailsURI));

            if (jobSummary.getStatus().isAbortable()) {
                final URI abortJobURI = new URI(JobResource.PATH + "/" + jobSummary.getId().toString() + "/abort");
                ret.put("abort", new RESTLink(abortJobURI));
            }

            if (jobDAO.hasStderr(jobSummary.getId())) {
                final URI jobStderrURI = new URI(JobResource.PATH + "/" + jobSummary.getId().toString() + "/stderr");
                ret.put("stderr", new RESTLink(jobStderrURI));
            }

            if (jobDAO.hasStdout(jobSummary.getId())) {
                final URI jobStdoutURI = new URI(JobResource.PATH + "/" + jobSummary.getId().toString() + "/stdout");
                ret.put("stdout", new RESTLink(jobStdoutURI));
            }

            return ret;
        } catch (URISyntaxException ex) {
            throw new WebApplicationException(ex);
        }
    }

    @GET
    @Path("{job-id}")
    @ApiOperation(
            value = "Get details of a job managed by the system.",
            code = 200,
            notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Job details found", response = JobDetailsResponse.class),
            @ApiResponse(code = 404, message = "The job could not be found", response = APIErrorMessage.class),
            @ApiResponse(code = 401, message = "Client not authorized to request job details", response = APIErrorMessage.class)
    })
    @PermitAll
    public Optional<JobDetailsResponse> fetchJobDetailsById(
            @Context
                    SecurityContext context,
            @ApiParam(value = "The job's ID")
            @PathParam("job-id")
            @NotNull
                    JobId jobId) {

        if (jobId == null) {
            throw new WebApplicationException("Job ID is null", 400);
        } else {
            final Optional<JobDetailsResponse> resp = jobDAO.getJobDetailsById(jobId);

            // Add REST links
            final Optional<JobDetailsResponse> respWithRESTLinks =
                    resp.map(jobDetailsResponse -> {
                        try {
                            if (jobDetailsResponse.latestStatus().isAbortable()) {
                                final URI abortURI = new URI(PATH + "/" + jobDetailsResponse.getId().toString() + "/abort");
                                jobDetailsResponse.getLinks().put("abort", new RESTLink(abortURI));
                            }
                            return jobDetailsResponse;
                        } catch (URISyntaxException ex) {
                            throw new WebApplicationException(ex);
                        }
                    });

            return respWithRESTLinks;
        }
    }

    @POST
    @ApiOperation(
            value = "Submit a new job",
            code = 200,
            notes = "Attempt to submit a new job to the system. The system will check the job against " +
                    "the job spec specified in the request. If it does not match, the request will be " +
                    "immediately rejected by the server. Otherwise, the request will be immediately accepted " +
                    "by the server. Note: the server accepting the job is only indicates that the request " +
                    "matches the job spec. It does not guarantee that the underlying job will complete " +
                    "successfully.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Job request accepted", response = JobRequestResponse.class),
            @ApiResponse(code = 400, message = "Invalid or malformed job request", response = APIErrorMessage.class)
    })
    @Produces("application/json")
    @Consumes("application/json")
    @PermitAll
    public JobRequestResponse submitJob(
            @Context
                    SecurityContext context,
            @ApiParam(value = "The job request")
            @NotNull
            @Valid
                    APIJobRequest APIJobRequest) {

        final UserId userId = new UserId(context.getUserPrincipal().getName());

        return validateAPIRequest(APIJobRequest, jobSpecConfigurationDAO, userId).visit(
                new EitherVisitorT<ValidJobRequest, List<ValidationError>, JobRequestResponse>() {
                    @Override
                    public JobRequestResponse whenLeft(ValidJobRequest left) {
                        final JobId jobId = jobManagerActions.submit(left).getLeft();

                        return new JobRequestResponse(jobId, new HashMap<>());
                    }

                    @Override
                    public JobRequestResponse whenRight(List<ValidationError> right) {
                        throw new WebApplicationException("Validation errors were found in the request: " + Helpers.commaSeparatedList(right), 400);
                    }
                });
    }

    public static Either<ValidJobRequest, List<ValidationError>> validateAPIRequest(
            APIJobRequest APIJobRequest,
            JobSpecConfigurationDAO jobSpecConfigurationDAO,
            UserId userId) {

        if (APIJobRequest == null)
            throw new WebApplicationException("Job id was null", 400);

        final Optional<JobSpec> maybeJobSchemaConfiguration =
                jobSpecConfigurationDAO.getJobSpecConfigurationById(APIJobRequest.getSpec());

        if (!maybeJobSchemaConfiguration.isPresent())
            throw new WebApplicationException(
                    "The specified schema id (" + APIJobRequest.getSpec() +
                            ") could not be found. Are you sure it's available?");

        return ValidJobRequest.tryCreate(maybeJobSchemaConfiguration.get(), userId, APIJobRequest);
    }

    @POST
    @Path("/{job-id}/abort")
    @ApiOperation(
            value = "Abort a running job",
            notes = "Abort a job, stopping it or removing it from the job execute. The job's status " +
                    "should immediately change to aborting. However, full job abortion is not guaranteed " +
                    "to be immediate. This is because the underlying job may take time to close gracefully " +
                    "or because the system itself has a short delay before forcibly killing the job outright.")
    @Produces("application/json")
    @PermitAll
    public void abortJob(
            @Context
                    SecurityContext context,
            @ApiParam(value = "ID of the job to abort")
            @PathParam("job-id")
            @NotNull
                    JobId jobId) {

        if (jobId == null) throw new WebApplicationException("Job ID cannot be null", 400);

        if (jobDAO.jobExists(jobId)) {
            if (jobManagerActions.tryAbort(jobId)) return;
            else throw new WebApplicationException("Job cannot be aborted", 400);
        } else throw new WebApplicationException("Job cannot be found", 400);
    }

    @GET
    @Path("/{job-id}/stdout")
    @ApiOperation(
            value = "Get a job's standard output",
            notes = "Get a job's standard output, if available. A job that has not yet started will not have a standard output and, " +
                    "therefore, this method will return a 404. There is no guarantee that all running/finished jobs will have standard output " +
                    "data. This is because administrative and cleanup routines may dequeue a job's output in order to save space on the server. ")
    @Produces("application/octet-stream")
    @PermitAll
    public Response fetchJobStdoutById(
            @Context
                    SecurityContext context,
            @ApiParam(value = "ID of the job to get stdout for")
            @PathParam("job-id")
            @NotNull
            JobId jobId) {

        if (jobId == null) throw new WebApplicationException("Job ID cannot be null", 400);

        final Optional<BinaryData> maybeStdoutData = jobDAO.getStdout(jobId);

        if (maybeStdoutData.isPresent()) {
            final BinaryData stdoutData = maybeStdoutData.get();

            final StreamingOutput body = outputStream -> IOUtils.copy(stdoutData.getData(), outputStream);

            return Response.ok(body, "application/octet-stream")
                    .header("Content-Length", stdoutData.getSizeOf())
                    .build();
        } else {
            final APIErrorMessage APIErrorMessage = new APIErrorMessage(jobId + ": could not be found", "404");
            return Response.status(404).entity(Entity.json(APIErrorMessage)).build();
        }
    }

    @GET
    @Path("/{job-id}/stderr")
    @ApiOperation(
            value = "Get the job's standard error",
            notes = "Get the job's standard error, if available. A job that has not yet starrted will not have a standard error and, " +
                    "therefore, this method will return a 404. There is no guarantee that all running/finished jobs will have standard " +
                    "error data. This is because administrative and cleanup routines may dequeue a job's output in order to save space on " +
                    "the server.")
    @Produces("application/octet-stream")
    @PermitAll
    public Response fetchJobStderrById(
            @Context
                    SecurityContext context,
            @ApiParam(value = "ID of the job to get stderr for")
            @PathParam("job-id")
            @NotNull
            JobId jobId) {

        if (jobId == null) throw new WebApplicationException("Job ID cannot be null", 400);

        final Optional<BinaryData> maybeStderrData = jobDAO.getStderr(jobId);

        if (maybeStderrData.isPresent()) {
            final BinaryData stderrData = maybeStderrData.get();
            final StreamingOutput body = outputStream -> IOUtils.copy(stderrData.getData(), outputStream);

            return Response.ok(body, "application/octet-stream")
                    .header("Content-Length", stderrData.getSizeOf())
                    .build();
        } else {
            final APIErrorMessage APIErrorMessage = new APIErrorMessage(jobId + ": could not find stderr", "404");
            return Response.status(404).entity(Entity.json(APIErrorMessage)).build();
        }
    }
}
