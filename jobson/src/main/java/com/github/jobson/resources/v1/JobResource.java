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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jobson.Constants;
import com.github.jobson.Helpers;
import com.github.jobson.api.v1.*;
import com.github.jobson.dao.jobs.JobDAO;
import com.github.jobson.dao.jobs.JobDetails;
import com.github.jobson.dao.jobs.ReadonlyJobDAO;
import com.github.jobson.dao.specs.JobSpecConfigurationDAO;
import com.github.jobson.jobinputs.JobExpectedInputId;
import com.github.jobson.jobs.JobId;
import com.github.jobson.jobs.JobManagerActions;
import com.github.jobson.jobs.jobstates.ValidJobRequest;
import com.github.jobson.specs.JobOutputId;
import com.github.jobson.specs.JobSpec;
import com.github.jobson.utils.BinaryData;
import com.github.jobson.utils.Either;
import com.github.jobson.utils.EitherVisitorT;
import com.github.jobson.utils.ValidationError;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.info.*;
import org.apache.commons.io.IOUtils;

import javax.annotation.security.PermitAll;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
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
import java.util.stream.Collectors;

import static com.github.jobson.Constants.DEFAULT_BINARY_MIME_TYPE;
import static com.github.jobson.Constants.HTTP_JOBS_PATH;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@OpenAPIDefinition(
        info = @Info(description = "Operations related to jobs"))
@Path(HTTP_JOBS_PATH)
@Produces("application/json")
public final class JobResource {

    private static final int MAX_PAGE_SIZE = 20;


    private final JobManagerActions jobManagerActions;
    private final JobSpecConfigurationDAO jobSpecConfigurationDAO;
    private final int defaultPageSize;
    private final JobDAO jobDAO;


    public JobResource(
            JobManagerActions jobManagerActions,
            JobDAO jobDAO,
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
    @Operation(
            summary = "Retrieve jobs managed by the system.",
            description = "Gets *some* of the jobs managed by the system. The response does not necessarily " +
                    "contain *all* the jobs managed by the system because pagination " +
                    "and client permissions may hide entries. ")
    @ApiResponses(value = {
           @ApiResponse(
                     responseCode = "200",
                     description = "Entries returned",
                     content = @Content(
                             schema = @Schema(implementation = APIJobDetailsCollection.class)
                     ))
    })
    @PermitAll
    public APIJobDetailsCollection getJobs(
            @Context
                    SecurityContext context,
            @Parameter(description = "The page number (0-indexed)")
            @QueryParam("page")
                    Optional<Integer> page,
            @Parameter(description = "The number of entries a response page should contain. Max page size is " + MAX_PAGE_SIZE)
            @QueryParam("page-size")
                    Optional<Integer> pageSize,
            @Parameter(description = "Client query string")
            @QueryParam("query")
                    Optional<String> query) {

        final int pageRequested = page.isPresent() ? page.get() : 0;
        final int pageSizeRequested = pageSize.isPresent() ? pageSize.get() : defaultPageSize;

        if (pageRequested < 0)
            throw new WebApplicationException("Page specified is negative - only positive numbers are allowed", 400);
        if (pageSizeRequested < 0)
            throw new WebApplicationException("Page size specified is negative - only positive numbers are allowed", 400);

        final List<JobDetails> jobs =
                query.isPresent() ?
                        jobDAO.getJobs(pageSizeRequested, pageRequested, query.get()) :
                        jobDAO.getJobs(pageSizeRequested, pageRequested);

        final List<APIJobDetails> apiJobDetailss = jobs
                .stream()
                .map(this::toJobResponse)
                .collect(toList());

        return new APIJobDetailsCollection(apiJobDetailss, emptyMap());
    }

    private APIJobDetails toJobResponse(JobDetails jobDetails) {
        final Map<String, APIRestLink> restLinks = generateRestLinks(jobDetails);
        return APIJobDetails.fromJobDetails(jobDetails, restLinks);
    }

    private Map<String, APIRestLink> generateRestLinks(JobDetails job) {
        try {
            final Map<String, APIRestLink> ret = generateRestLinks(job.getId());
            if (job.latestStatus().isAbortable()) {
                final URI abortJobURI = new URI(HTTP_JOBS_PATH + "/" + job.getId().toString() + "/abort");
                ret.put("abort", new APIRestLink(abortJobURI));
            }
            return ret;
        } catch (URISyntaxException ex) {
            throw new WebApplicationException(ex);
        }
    }

    private Map<String, APIRestLink> generateRestLinks(JobId jobId) {
        try {
            final HashMap<String, APIRestLink> ret = new HashMap<>();

            final URI jobDetailsURI = new URI(HTTP_JOBS_PATH + "/" + jobId.toString());
            ret.put("self", new APIRestLink(jobDetailsURI));

            final URI jobSpecURI = new URI(HTTP_JOBS_PATH + "/" + jobId.toString() + "/spec");
            ret.put("spec", new APIRestLink(jobSpecURI));

            if (jobDAO.hasJobInputs(jobId)) {
                final URI jobInputsURI =  new URI(HTTP_JOBS_PATH + "/" + jobId.toString() + "/inputs");
                ret.put("inputs", new APIRestLink(jobInputsURI));
            }

            final URI jobOutputsURI = new URI(HTTP_JOBS_PATH + "/" + jobId.toString() + "/outputs");
            ret.put("outputs", new APIRestLink(jobOutputsURI));

            if (jobDAO.hasStderr(jobId)) {
                final URI jobStderrURI = new URI(HTTP_JOBS_PATH + "/" + jobId.toString() + "/stderr");
                ret.put("stderr", new APIRestLink(jobStderrURI));
            }

            if (jobDAO.hasStdout(jobId)) {
                final URI jobStdoutURI = new URI(HTTP_JOBS_PATH + "/" + jobId.toString() + "/stdout");
                ret.put("stdout", new APIRestLink(jobStdoutURI));
            }

            return ret;
        } catch (URISyntaxException ex) {
            throw new WebApplicationException(ex);
        }
    }

    @GET
    @Path("{job-id}")
    @Operation(
            summary = "Get details of a job managed by the system.",
            description = "")
    @ApiResponses(value = {
            @ApiResponse(
                     responseCode = "200",
                     description = "Job details found",
                     content = @Content(
                             schema = @Schema(implementation = APIJobDetails.class)
                     )),
            @ApiResponse(
                     responseCode = "404",
                     description = "The job could not be found",
                     content = @Content(
                             schema = @Schema(implementation = APIErrorMessage.class)
                     )),
            @ApiResponse(
                     responseCode = "401",
                     description = "Client not authorized to request job details",
                     content = @Content(
                             schema = @Schema(implementation = APIErrorMessage.class)
                     ))
    })
    @PermitAll
    public Optional<APIJobDetails> getJobDetailsById(
            @Context
                    SecurityContext context,
            @Parameter(description = "The job's ID")
            @PathParam("job-id")
            @NotNull
                    JobId jobId) {

        if (jobId == null)
            throw new WebApplicationException("Job ID is null", 400);

        return jobDAO.getJobDetailsById(jobId).map(this::toJobResponse);
    }

    @DELETE
    @Path("{job-id}")
    @Operation(
            summary = "Delete a job from the system",
            description = "Deletes a job from the system, removing **all** job data. Running jobs are aborted before deletion."
    )
    @PermitAll
    public int deleteJob(
            @Context
                    SecurityContext context,
            @Parameter(description = "The job's ID")
            @PathParam("job-id")
            @NotNull
                    JobId jobId) {

        if (jobId == null)
            throw new WebApplicationException("Job ID is null", 400);

        // ensure the job is aborted before deleting it: stops dangling IO writes
        jobManagerActions.tryAbort(jobId);
        jobDAO.remove(jobId);

        return 200;
    }

    @POST
    @Operation(
            summary = "Submit a new job",
            description = "Attempt to submit a new job to the system. The system will check the job against " +
                    "the job spec specified in the request. If it does not match, the request will be " +
                    "immediately rejected by the server. Otherwise, the request will be immediately accepted " +
                    "by the server. Note: the server accepting the job is only indicates that the request " +
                    "matches the job spec. It does not guarantee that the underlying job will complete " +
                    "successfully.")
    @ApiResponses(value = {
            @ApiResponse(
                     responseCode = "200",
                     description = "Job request accepted",
                     content = @Content(
                             schema = @Schema(implementation = APIJobCreatedResponse.class)
                     )),
            @ApiResponse(
                     responseCode = "400",
                     description = "Invalid or malformed job request",
                     content = @Content(
                             schema = @Schema(implementation = APIErrorMessage.class)
                     ))
    })
    @PermitAll
    public APIJobCreatedResponse submitJob(
            @Context
                    SecurityContext context,
            @Parameter(description = "The job request")
            @NotNull
            @Valid
                    APIJobRequest apiJobRequest) {

        final UserId userId = new UserId(context.getUserPrincipal().getName());

        return validateAPIRequest(apiJobRequest, jobSpecConfigurationDAO, userId).visit(
                new EitherVisitorT<ValidJobRequest, List<ValidationError>, APIJobCreatedResponse>() {
                    @Override
                    public APIJobCreatedResponse whenLeft(ValidJobRequest left) {
                        final JobId jobId = jobManagerActions.submit(left).getLeft();

                        return new APIJobCreatedResponse(jobId, generateRestLinks(jobId));
                    }

                    @Override
                    public APIJobCreatedResponse whenRight(List<ValidationError> right) {
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
                jobSpecConfigurationDAO.getJobSpecById(APIJobRequest.getSpec());

        if (!maybeJobSchemaConfiguration.isPresent())
            throw new WebApplicationException(
                    "The specified schema id (" + APIJobRequest.getSpec() +
                            ") could not be found. Are you sure it's available?");

        return ValidJobRequest.tryCreate(maybeJobSchemaConfiguration.get(), userId, APIJobRequest);
    }

    @POST
    @Path("/{job-id}/abort")
    @Operation(
            summary = "Abort a running job",
            description = "Abort a job, stopping it or removing it from the job execute. The job's status " +
                    "should immediately change to aborting. However, full job abortion is not guaranteed " +
                    "to be immediate. This is because the underlying job may take time to close gracefully " +
                    "or because the system itself has a short delay before forcibly killing the job outright.")
    @PermitAll
    public void abortJob(
            @Context
                    SecurityContext context,
            @Parameter(description = "ID of the job to abort")
            @PathParam("job-id")
            @NotNull
                    JobId jobId) {

        if (jobId == null)
            throw new WebApplicationException("Job ID cannot be null", 400);

        if (jobDAO.jobExists(jobId)) {
            if (jobManagerActions.tryAbort(jobId)) return;
            else throw new WebApplicationException("Job cannot be aborted", 400);
        } else throw new WebApplicationException("Job cannot be found", 400);
    }

    @GET
    @Path("/{job-id}/stdout")
    @Operation(
            summary = "Get a job's standard output",
            description = "Get a job's standard output, if available. A job that has not yet started will not have a standard output and, " +
                    "therefore, this method will return a 404. There is no guarantee that all running/finished jobs will have standard output " +
                    "data. This is because administrative and cleanup routines may dequeue a job's output in order to save space on the server. ")
    @Produces(DEFAULT_BINARY_MIME_TYPE)
    @PermitAll
    public Response fetchJobStdoutById(
            @Context
                    SecurityContext context,
            @Parameter(description = "ID of the job to get stdout for")
            @PathParam("job-id")
            @NotNull
            JobId jobId) {

        if (jobId == null) throw new WebApplicationException("Job ID cannot be null", 400);

        return generateBinaryDataResponse(jobId, jobDAO.getStdout(jobId));
    }

    private Response generateBinaryDataResponse(JobId jobId, Optional<BinaryData> maybeBinaryData) {
        if (maybeBinaryData.isPresent()) {
            final BinaryData binaryData = maybeBinaryData.get();

            final StreamingOutput body = outputStream -> {
                try {
                    IOUtils.copyLarge(binaryData.getData(), outputStream);
                } catch (IOException ex) {
                    // This *usually* happens becuse the client closed the TCP
                    // connection, which isn't *exceptional*.
                } finally {
                    binaryData.getData().close();
                }
            };

            final Response.ResponseBuilder b =
                    Response.ok(body, binaryData.getMimeType())
                            .header("Content-Length", binaryData.getSizeOf());

            if (binaryData.getSizeOf() > Constants.MAX_JOB_OUTPUT_SIZE_IN_BYTES_BEFORE_DISABLING_COMPRESSION)
                b.header("Content-Encoding", "identity");

            return b.build();
        } else {
            return Response.status(404).build();
        }
    }

    @GET
    @Path("/{job-id}/stderr")
    @Operation(
            summary = "Get the job's standard error",
            description = "Get the job's standard error, if available. A job that has not yet started will not have a standard error and, " +
                    "therefore, this method will return a 404. There is no guarantee that all running/finished jobs will have standard " +
                    "error data. This is because administrative and cleanup routines may dequeue a job's output in order to save space on " +
                    "the server.")
    @Produces(DEFAULT_BINARY_MIME_TYPE)
    @PermitAll
    public Response fetchJobStderrById(
            @Context
                    SecurityContext context,
            @Parameter(description = "ID of the job to get stderr for")
            @PathParam("job-id")
            @NotNull
            JobId jobId) {

        if (jobId == null)
            throw new WebApplicationException("Job ID cannot be null", 400);

        return generateBinaryDataResponse(jobId, jobDAO.getStderr(jobId));
    }

    @GET
    @Path("/{job-id}/spec")
    @Operation(
            summary = "Get the spec the job was submitted against",
            description = "Get the spec the job was submitted against. Note: This returns the spec as it was when the " +
                    "job was submitted. Any subsequent updates to the spec will not be in the spec returned by this API call.")
    @PermitAll
    public Optional<APIJobSpec> fetchJobSpecJobWasSubmittedAgainst(
            @Context
                    SecurityContext context,
            @Parameter(description = "ID of the job to get the spec for")
            @PathParam("job-id")
            @NotNull
                    JobId jobId) {

        if (jobId == null)
            throw new WebApplicationException("Job ID cannot be null", 400);

        return jobDAO.getSpecJobWasSubmittedAgainst(jobId)
                .map(APIJobSpec::fromJobSpec);
    }

    @GET
    @Path("/{job-id}/inputs")
    @Operation(
            summary = "Get the job's inputs",
            description = "Get the inputs that were supplied when the job was submitted.")
    @PermitAll
    public Optional<Map<JobExpectedInputId, JsonNode>> fetchJobInputs(
            @Context
                    SecurityContext context,
            @Parameter(description = "ID of the job to get inputs for")
            @PathParam("job-id")
            @NotNull
                    JobId jobId) {

        if (jobId == null)
            throw new WebApplicationException("Job ID cannot be null", 400);

        return jobDAO.getJobInputs(jobId);
    }

    @GET
    @Path("/{job-id}/outputs")
    @Operation(
            summary = "Get the outputs produced by the job",
            description = "Gets all the outputs produced by the job. If the job has not *written* any outputs (even if specified) " +
                    "then an empty map is returned. If the job does not exist, a 404 is returned")
    @PermitAll
    public APIJobOutputCollection fetchJobOutputs(
            @Context
                    SecurityContext context,
            @Parameter(description = "ID of the job to get the outputs for")
            @PathParam("job-id")
            @NotNull
                    JobId jobId) {

        if (!jobDAO.jobExists(jobId))
            throw new WebApplicationException(jobId + ": does not exist", 404);

        final List<APIJobOutput> entries =  jobDAO
                .getJobOutputs(jobId)
                .stream()
                .map(jobOutput -> {
                    final String href = HTTP_JOBS_PATH + "/" + jobId + "/outputs/" + jobOutput.getId().toString();
                    return APIJobOutput.fromJobOutput(href, jobOutput);
                })
                .collect(Collectors.toList());

        return new APIJobOutputCollection(entries);
    }

    @GET
    @Path("/{job-id}/outputs/{output-id}")
    @Operation(
            summary = "Get an output produced by the job",
            description = "Gets an output produced by the job. If the job has not written this output, of it it has been " +
                    "subsequently deleted, a 404 shall be returned")
    @PermitAll
    public Response fetchJobOutput(
            @Context
                    SecurityContext context,
            @Parameter(description = "ID of the job to get the output for")
            @PathParam("job-id")
            @NotNull
                    JobId jobId,
            @Parameter(description = "ID of the output")
            @PathParam("output-id")
            @NotNull
                    JobOutputId outputId) {

        if (!jobDAO.jobExists(jobId))
            throw new WebApplicationException(jobId + ": does not exist", 404);

        final Optional<BinaryData> maybeJobOutput = jobDAO.getOutput(jobId, outputId);

        if (!maybeJobOutput.isPresent())
            throw new WebApplicationException(jobId + ": " + outputId + ": does not exist", 404);

        return generateBinaryDataResponse(jobId, maybeJobOutput);
    }
}
