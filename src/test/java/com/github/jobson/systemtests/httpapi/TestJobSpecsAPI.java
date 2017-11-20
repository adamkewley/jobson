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
import com.github.jobson.api.v1.APIJobSpec;
import com.github.jobson.api.v1.APIJobSpecSummaryCollection;
import com.github.jobson.config.ApplicationConfig;
import com.github.jobson.specs.JobSpec;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

import static com.github.jobson.Constants.HTTP_SPECS_PATH;
import static com.github.jobson.HttpStatusCodes.OK;
import static com.github.jobson.HttpStatusCodes.UNAUTHORIZED;
import static com.github.jobson.TestHelpers.readYAMLFixture;
import static com.github.jobson.systemtests.SystemTestHelpers.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public final class TestJobSpecsAPI {

    @ClassRule
    public static final DropwizardAppRule<ApplicationConfig> RULE = createStandardRule();


    @Test
    public void testUnauthorizedIfGETSummariesWithoutCredentials() {
        final Response response = generateRequest(RULE, HTTP_SPECS_PATH).get();
        assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    public void testOKIfGETSummariesWithCredentials() throws IOException {
        final Response resp = generateAuthenticatedRequest(RULE, HTTP_SPECS_PATH).get();
        assertThat(resp.getStatus()).isEqualTo(OK);
    }

    @Test
    public void testOKSummariesResponseContainsSummaries() throws IOException {
        final Response response = generateAuthenticatedRequest(RULE, HTTP_SPECS_PATH).get();
        response.readEntity(APIJobSpecSummaryCollection.class);
    }

    @Test
    public void testGetJobSpecSummariesContainsTheJobSpecsInTheJobSpecsFolder() throws IOException {
        final APIJobSpecSummaryCollection resp = generateAuthenticatedRequest(RULE, HTTP_SPECS_PATH)
                .get()
                .readEntity(APIJobSpecSummaryCollection.class);

        final List<JobSpec> specsProvidedWhenBooting =
                asList(readYAMLFixture("fixtures/systemtests/jobspecs.yml", JobSpec[].class));

        assertThat(resp.getEntries().size()).isEqualTo(specsProvidedWhenBooting.size());
    }


    @Test
    public void testGetJobSpecByIdHasAuthenticationErrorIfNotSignedIn() {
        final Response response = generateRequest(RULE,HTTP_SPECS_PATH + "/first-spec").get();
        assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    public void testGetJobSpecByIdReturnsAJobSpecIfSignedIn() throws IOException {
        final Response response = generateAuthenticatedRequest(RULE, HTTP_SPECS_PATH + "/first-spec")
                .get();

        assertThat(response.getStatus()).isEqualTo(OK);

        TestHelpers.readJSON(response.readEntity(String.class), APIJobSpec.class);
    }
}
