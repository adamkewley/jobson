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

package com.github.jobson.systemtests.httpapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.jobson.api.v1.*;
import com.github.jobson.config.ApplicationConfig;
import com.github.jobson.jobinputs.JobExpectedInputId;
import com.github.jobson.jobs.JobId;
import com.github.jobson.systemtests.SystemTestHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;

import static com.github.jobson.Constants.HTTP_JOBS_PATH;
import static com.github.jobson.Helpers.readJSON;
import static com.github.jobson.HttpStatusCodes.*;
import static com.github.jobson.TestHelpers.readJSONFixture;
import static com.github.jobson.jobs.JobStatus.ABORTED;
import static com.github.jobson.jobs.JobStatus.RUNNING;
import static com.github.jobson.systemtests.SystemTestHelpers.*;
import static java.lang.Thread.sleep;
import static java.util.Collections.singletonList;
import static javax.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;

public final class TestJobsAPI {

    @ClassRule
    public static final DropwizardAppRule<ApplicationConfig> RULE = SystemTestHelpers.createStandardRule();

    private static final APIJobRequest REQUEST_AGAINST_FIRST_SPEC;
    private static final APIJobRequest REQUEST_AGAINST_SECOND_SPEC;
    private static final APIJobRequest REQUEST_AGAINST_THIRD_SPEC;
    private static final APIJobRequest REQUEST_AGAINST_FOUTH_SPEC;
    private static final APIJobRequest REQUEST_AGAINST_FITH_SPEC;

    static {
        REQUEST_AGAINST_FIRST_SPEC = readJSONFixture(
                "fixtures/systemtests/request-against-first-spec.json",
                APIJobRequest.class);
        REQUEST_AGAINST_SECOND_SPEC = readJSONFixture(
                "fixtures/systemtests/request-against-second-spec.json",
                APIJobRequest.class);
        REQUEST_AGAINST_THIRD_SPEC = readJSONFixture(
                "fixtures/systemtests/request-against-third-spec.json",
                APIJobRequest.class);
        REQUEST_AGAINST_FOUTH_SPEC = readJSONFixture(
                "fixtures/systemtests/request-against-fourth-spec.json",
                APIJobRequest.class);
        REQUEST_AGAINST_FITH_SPEC = readJSONFixture(
                "fixtures/systemtests/request-against-fith-spec.json",
                APIJobRequest.class);
    }


    @Test
    public void testUnauthorizedIfCallingAPIWithoutCredentials() throws IOException {
        final Response response = generateRequest(RULE, HTTP_JOBS_PATH)
                .post(json(REQUEST_AGAINST_FIRST_SPEC));

        assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    public void testBadRequestIfAuthorizedButBadRequest() throws IOException {
        final Response response = generateAuthenticatedRequest(RULE, HTTP_JOBS_PATH)
                .post(json(singletonList("Not a request")));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void testOKForCorrectAuthorizedRequest() throws IOException {
        final Response response = generateAuthenticatedRequest(RULE, HTTP_JOBS_PATH)
                .post(json(REQUEST_AGAINST_FIRST_SPEC));

        assertThat(response.getStatus()).isEqualTo(OK);
    }

    @Test
    public void testCorrectRequestAgainst2ndSpecRespondsWithOK() throws IOException {
        final Response response = generateAuthenticatedRequest(RULE, HTTP_JOBS_PATH)
                .post(json(REQUEST_AGAINST_SECOND_SPEC));

        assertThat(response.getStatus()).isEqualTo(OK);
    }

    @Test
    public void testOKResponseContainsJobResponse() throws IOException {
        final Response response = generateAuthenticatedRequest(RULE, HTTP_JOBS_PATH)
                .post(json(REQUEST_AGAINST_SECOND_SPEC));

        response.readEntity(APIJobCreatedResponse.class);
    }

    @Test
    public void testCanGETJobDetailsForANewJob() throws IOException {
        final Response response = generateAuthenticatedRequest(RULE, HTTP_JOBS_PATH)
                .post(json(REQUEST_AGAINST_SECOND_SPEC));

        final JobId jobId = response.readEntity(APIJobCreatedResponse.class).getId();

        final Invocation.Builder requestForDetailsBuilder =
                generateAuthenticatedRequest(RULE, jobResourceSubpath(jobId));

        final Response detailsResponse = requestForDetailsBuilder.get();

        assertThat(detailsResponse.getStatus()).isEqualTo(OK);
        readJSON(detailsResponse.readEntity(String.class), APIJobDetails.class);
    }

    private String jobResourceSubpath(Object subpath) {
        return HTTP_JOBS_PATH + "/" + subpath.toString();
    }

    @Test
    public void testCanAbortAJob() throws IOException {
        final Response response = generateAuthenticatedRequest(RULE, HTTP_JOBS_PATH)
                .post(json(REQUEST_AGAINST_THIRD_SPEC));

        final JobId jobId = response.readEntity(APIJobCreatedResponse.class).getId();

        final APIJobDetails APIJobDetailsDetails = fetchJobDetails(jobId);

        assertThat(APIJobDetailsDetails.latestStatus()).isEqualTo(RUNNING);

        final Invocation.Builder abortionRequest = generateAuthenticatedRequest(
                RULE, jobResourceSubpath(jobId + "/abort"));

        final Response abortionResponse = abortionRequest.post(json(""));

        assertThat(abortionResponse.getStatus()).isEqualTo(NO_CONTENT);

        final com.github.jobson.api.v1.APIJobDetails jobDetailsAfterAbortion = fetchJobDetails(jobId);

        assertThat(jobDetailsAfterAbortion.latestStatus()).isEqualTo(ABORTED);
    }

    private APIJobDetails fetchJobDetails(JobId jobId) throws IOException {
        final Invocation.Builder builder = generateRequest(RULE, jobResourceSubpath(jobId));
        authenticate(builder);
        return readJSON(builder.get().readEntity(String.class), APIJobDetails.class);
    }

    @Test
    public void testCanGETJobSummaries() throws IOException {
        final Invocation.Builder builder =
                generateAuthenticatedRequest(RULE, HTTP_JOBS_PATH);

        for (int i = 0; i < 10; i++) {
            builder.post(json(REQUEST_AGAINST_SECOND_SPEC), APIJobCreatedResponse.class);
        }

        final Response jobSummariesResponse =
                generateAuthenticatedRequest(RULE, HTTP_JOBS_PATH).get();

        assertThat(jobSummariesResponse.getStatus()).isEqualTo(OK);

        final APIJobDetailsCollection jobSummaries =
                readJSON(jobSummariesResponse.readEntity(String.class), APIJobDetailsCollection.class);

        assertThat(jobSummaries.getEntries().isEmpty()).isFalse();
    }

    @Test
    public void testCanGETStdout() throws IOException, InterruptedException {
        final JobId jobId = generateAuthenticatedRequest(RULE, HTTP_JOBS_PATH)
                .post(json(REQUEST_AGAINST_FIRST_SPEC))
                .readEntity(APIJobCreatedResponse.class)
                .getId();

        // Give the job a chance to spin up and write to stdout
        // TODO: Websocket hook this instead.
        sleep(100);

        final Response stdoutResponse =
                generateAuthenticatedRequest(RULE, jobResourceSubpath(jobId + "/stdout"))
                        .get();

        assertThat(stdoutResponse.getStatus()).isEqualTo(OK);

        final byte[] stdoutBytes = stdoutResponse.readEntity(byte[].class);

        assertThat(stdoutBytes).isEqualTo("hello world\n".getBytes()); // From the spec execution
    }

    @Test
    public void testCanListJobOutputs() throws InterruptedException, IOException {
        final JobId jobId = generateAuthenticatedRequest(RULE, HTTP_JOBS_PATH)
                .post(json(REQUEST_AGAINST_FOUTH_SPEC))
                .readEntity(APIJobCreatedResponse.class)
                .getId();

        // TODO: Websocket hook this instead.
        sleep(100);

        final Response jobOutputsResponse =
                generateAuthenticatedRequest(RULE, jobResourceSubpath(jobId + "/outputs")).get();

        assertThat(jobOutputsResponse.getStatus()).isEqualTo(OK);

        final Map<String, APIJobOutput> parsedResponse =
                readJSON(jobOutputsResponse.readEntity(String.class), new TypeReference<Map<String, APIJobOutput>>() {});

        assertThat(parsedResponse.get("outFile").getMimeType().get()).isEqualTo("text/plain");
        assertThat(parsedResponse.get("outFile").getName().get()).isEqualTo("Output Name");
        assertThat(parsedResponse.get("outFile").getDescription().get()).isEqualTo("Output Description");
    }

    @Test
    public void testCanGetJobOutput() throws InterruptedException {
        final JobId jobId = generateAuthenticatedRequest(RULE, HTTP_JOBS_PATH)
                .post(json(REQUEST_AGAINST_FOUTH_SPEC))
                .readEntity(APIJobCreatedResponse.class)
                .getId();

        // TODO: Websocket hook this instead.
        sleep(100);

        final Response jobOutputsResponse =
                generateAuthenticatedRequest(RULE, jobResourceSubpath(jobId + "/outputs/outFile")).get();

        assertThat(jobOutputsResponse.getStatus()).isEqualTo(OK);
        assertThat(jobOutputsResponse.getHeaderString("Content-Type")).isEqualTo("text/plain");
    }

    @Test
    public void testCanGetTemplatedJobOutput() throws InterruptedException {
        final JobId jobId = generateAuthenticatedRequest(RULE, HTTP_JOBS_PATH)
                .post(json(REQUEST_AGAINST_FITH_SPEC))
                .readEntity(APIJobCreatedResponse.class)
                .getId();

        sleep(100);

        final Response jobOutputsResponse =
                generateAuthenticatedRequest(RULE, jobResourceSubpath(jobId + "/outputs/" + jobId)).get();

        assertThat(jobOutputsResponse.getStatus()).isEqualTo(OK);
        assertThat(jobOutputsResponse.getHeaderString("Content-Type")).isEqualTo("text/plain");
    }

    @Test
    public void testCanGetJobInputs() throws IOException {
        final APIJobRequest req = REQUEST_AGAINST_FIRST_SPEC;

        final JobId jobId = generateAuthenticatedRequest(RULE, HTTP_JOBS_PATH)
                .post(json(req))
                .readEntity(APIJobCreatedResponse.class)
                .getId();

        final Response jobInputsResponse =
                generateAuthenticatedRequest(RULE, jobResourceSubpath(jobId + "/inputs"))
                .get();

        assertThat(jobInputsResponse.getStatus()).isEqualTo(OK);

        final String responseJson = jobInputsResponse.readEntity(String.class);

        final Map<JobExpectedInputId, JsonNode> inputsReturned =
                readJSON(responseJson, new TypeReference<Map<JobExpectedInputId, JsonNode>>() {});

        assertThat(inputsReturned).isEqualTo(req.getInputs());
    }
}
