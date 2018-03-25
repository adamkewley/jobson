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

import com.github.jobson.Constants;
import com.github.jobson.TestConstants;
import com.github.jobson.TestHelpers;
import com.github.jobson.api.APIRootResponse;
import com.github.jobson.config.ApplicationConfig;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static com.github.jobson.Constants.HTTP_ROOT;
import static com.github.jobson.HttpStatusCodes.UNAUTHORIZED;
import static com.github.jobson.TestHelpers.assertHasKeyWithValue;
import static com.github.jobson.systemtests.SystemTestHelpers.createStandardRule;
import static com.github.jobson.systemtests.SystemTestHelpers.generateAuthenticatedRequest;
import static com.github.jobson.systemtests.SystemTestHelpers.generateRequest;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public final class TestRootAPI {

    @ClassRule
    public static final DropwizardAppRule<ApplicationConfig> RULE = createStandardRule();


    @Test
    public void testUnauthorizedIfGETRootWithoutCredentials() {
        final Response response = generateRequest(RULE, HTTP_ROOT).get();
        assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    public void testAuthorizedRequestReturns200() {
        final Response response = generateAuthenticatedRequest(RULE, HTTP_ROOT).get();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    public void testGetRootAPIReturnsAnRootAPIResponse() {
        final Response response = generateAuthenticatedRequest(RULE, HTTP_ROOT).get();

        final String json = response.readEntity(String.class);
        final APIRootResponse parsedResponse = TestHelpers.readJSON(json, APIRootResponse.class);

        assertThat(parsedResponse).isNotNull();
    }

    @Test
    public void testGetRootAPIContainsLinkToV1API() {
        final Response response = generateAuthenticatedRequest(RULE, HTTP_ROOT).get();

        final String json = response.readEntity(String.class);
        final APIRootResponse parsedResponse = TestHelpers.readJSON(json, APIRootResponse.class);

        assertHasKeyWithValue(parsedResponse.getLinks(), "v1", Constants.HTTP_V1_ROOT);
    }
}
