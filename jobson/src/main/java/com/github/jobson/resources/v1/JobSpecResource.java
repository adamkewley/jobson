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

import com.github.jobson.api.v1.*;
import com.github.jobson.dao.specs.JobSpecDAO;
import com.github.jobson.dao.specs.JobSpecSummary;
import com.github.jobson.specs.JobSpecId;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.media.*;

import javax.annotation.security.PermitAll;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.jobson.Constants.HTTP_SPECS_PATH;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@OpenAPIDefinition(
                   info = @Info(description = "Operations related to job specifications"))
@Path(HTTP_SPECS_PATH)
@Produces("application/json")
public final class JobSpecResource {


    private final JobSpecDAO jobSpecDAO;
    private final int defaultPageSize;


    public JobSpecResource(JobSpecDAO jobSpecDAO, int defaultPageSize) {
        requireNonNull(jobSpecDAO);

        if (defaultPageSize < 0)
            throw new RuntimeException("Default page size cannot be negative");

        this.jobSpecDAO = jobSpecDAO;
        this.defaultPageSize = defaultPageSize;
    }


    @GET
    @Operation(
            summary = "Get summaries of the job specs exposed by the system",
            description = "Returns an object that contains summaries of *some* of the job specs exposed by the system. " +
                    "The response does not necessarily contain summaries for *all* job specs exposed by the system. " +
                    "This is because pagination and client permissions may hide job specs. If further pages of job specs are " +
                    "available, links shall be set to contain hrefs which the client may use to fetch more specs." +
                    "The server may reorder job spec summaries based on its configuration or knowledge about the user. " +
                    "If a client sets a query string, the server will respond appropriately; however, the same rules apply.")
    @ApiResponses(value = {
            @ApiResponse(
                     responseCode = "200",
                     description = "Job summaries returned",
                     content = @Content(
                             schema = @Schema(implementation = APIJobSpecSummaryCollection.class)
                     )),
    })
    @PermitAll
    public APIJobSpecSummaryCollection fetchJobSpecSummaries(
            @Context
                    SecurityContext context,
            @Parameter(description = "The page number (0-indexed)")
            @QueryParam("page")
                    Optional<Integer> page,
            @Parameter(description = "The number of entries a response page should contain.")
            @QueryParam("page-size")
                    Optional<Integer> pageSize,
            @Parameter(description = "Client query string")
            @QueryParam("query")
                    Optional<String> query) {

        final int requestedPage = page.isPresent() ? page.get() : 0;
        final int requestedPageSize = pageSize.isPresent() ? pageSize.get() : defaultPageSize;

        if (requestedPage < 0)
            throw new WebApplicationException("Requested page cannot be negative", 400);
        if (requestedPageSize < 0)
            throw new WebApplicationException("Requested page size cannot be negative", 400);

        final List<JobSpecSummary> jobSummaries =
                query.isPresent() ?
                        jobSpecDAO.getJobSpecSummaries(requestedPageSize, requestedPage, query.get()) :
                        jobSpecDAO.getJobSpecSummaries(requestedPageSize, requestedPage);

        final List<APIJobSpecSummary> apiJobSpecSummaries =
                jobSummaries.stream().map(summary -> {
                    try {
                        final Map<String, APIRestLink> restLinks = singletonMap(
                                "details",
                                new APIRestLink(new URI(HTTP_SPECS_PATH + "/" + summary.getId().toString())));
                        return APIJobSpecSummary.fromJobSpecSummary(summary, restLinks);
                    } catch (URISyntaxException ex) {
                        throw new WebApplicationException(ex);
                    }
                }).collect(toList());

        return new APIJobSpecSummaryCollection(apiJobSpecSummaries, emptyMap());
    }

    @GET
    @Path("{job-spec-id}")
    @Operation(
            summary = "Get a a job spec.",
            description = "If found, returns a job spec. A job spec describes declaratively what a " +
                    "job needs in order to run.")
    @ApiResponses(value = {
            @ApiResponse(
                     responseCode = "200",
                     description = "Job specification found and returned",
                     content = @Content(
                             schema = @Schema(implementation = APIJobSpec.class)
                     )),
            @ApiResponse(
                     responseCode = "404",
                     description = "The job specification cannot be found",
                     content = @Content(
                             schema = @Schema(implementation = APIErrorMessage.class)
                     ))
    })
    @PermitAll
    public Optional<APIJobSpec> fetchJobSpecDetailsById(
            @Context
                    SecurityContext context,
            @Parameter(description = "The job spec's ID")
            @PathParam("job-spec-id")
            @NotNull
                    JobSpecId jobSpecId) throws IOException {

        if (jobSpecId == null) throw new WebApplicationException("Job Spec ID cannot be null", 400);

        return jobSpecDAO.getJobSpecById(jobSpecId).map(APIJobSpec::fromJobSpec);
    }
}
