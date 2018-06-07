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

import com.github.jobson.TestHelpers;
import com.github.jobson.api.v1.APIV1RootResponse;
import com.github.jobson.config.ApplicationConfig;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static com.github.jobson.Constants.HTTP_V1_ROOT;
import static com.github.jobson.HttpStatusCodes.OK;
import static com.github.jobson.HttpStatusCodes.UNAUTHORIZED;
import static com.github.jobson.systemtests.SystemTestHelpers.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public final class TestV1RootAPI {

    @ClassRule
    public static final DropwizardAppRule<ApplicationConfig> RULE = createStandardRule();


    @Test
    public void testUnauthorizedIfGETSummariesWithoutCredentials() {
        final Response response = generateRequest(RULE, HTTP_V1_ROOT).get();
        assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    public void testOKIfGETSummariesWithCredentials() {
        final Response resp = generateAuthenticatedRequest(RULE, HTTP_V1_ROOT).get();
        assertThat(resp.getStatus()).isEqualTo(OK);
    }

    @Test
    public void testResponseParsesToAnAPIRootResponse() {
        final Response resp = generateAuthenticatedRequest(RULE, HTTP_V1_ROOT).get();
        final String json = resp.readEntity(String.class);
        final APIV1RootResponse parsedResp = TestHelpers.readJSON(json, APIV1RootResponse.class);
        assertThat(parsedResp).isNotNull();
    }

    @Test
    public void testResponseContainsJobsLinks() {
        final Response resp = generateAuthenticatedRequest(RULE, HTTP_V1_ROOT).get();
        final String json = resp.readEntity(String.class);
        final APIV1RootResponse parsedResp = TestHelpers.readJSON(json, APIV1RootResponse.class);
        assertThat(parsedResp.getLinks().containsKey("jobs")).isTrue();
    }

    @Test
    public void testResponseContainsSpecsLinks() {
        final Response resp = generateAuthenticatedRequest(RULE, HTTP_V1_ROOT).get();
        final String json = resp.readEntity(String.class);
        final APIV1RootResponse parsedResp = TestHelpers.readJSON(json, APIV1RootResponse.class);
        assertThat(parsedResp.getLinks().containsKey("specs")).isTrue();
    }

    @Test
    public void testResponseContainsCurrentUserLinks() {
        final Response resp = generateAuthenticatedRequest(RULE, HTTP_V1_ROOT).get();
        final String json = resp.readEntity(String.class);
        final APIV1RootResponse parsedResp = TestHelpers.readJSON(json, APIV1RootResponse.class);
        assertThat(parsedResp.getLinks().containsKey("current-user")).isTrue();
    }

    @Test
    public void testApiEmitsPrettifiedJson() {
        final Response resp = generateAuthenticatedRequest(RULE, HTTP_V1_ROOT).get();
        final String json = resp.readEntity(String.class);
        assertThat(json.split(System.lineSeparator()).length).isGreaterThan(1);
    }
}
