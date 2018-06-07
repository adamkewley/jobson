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

import com.github.jobson.TestHelpers;
import com.github.jobson.api.v1.APIV1RootResponse;
import org.junit.Test;

import javax.ws.rs.core.SecurityContext;

import static com.github.jobson.Constants.*;
import static com.github.jobson.TestHelpers.assertHasKeyWithValue;
import static org.assertj.core.api.Assertions.assertThat;

public final class V1RootResourceTest {

    @Test
    public void testGetReturnsAResponse() {
        assertThat(createRootResourceAndGetResponse()).isNotNull();
    }

    private APIV1RootResponse createRootResourceAndGetResponse() {
        final V1RootResource v1RootResource = new V1RootResource();
        final SecurityContext securityContext = TestHelpers.generateSecureSecurityContext();
        return v1RootResource.get(securityContext);
    }

    @Test
    public void testGetReturnsAResponseThatHasAJobsLink() {
        final APIV1RootResponse APIV1RootResponse = createRootResourceAndGetResponse();
        assertHasKeyWithValue(APIV1RootResponse.getLinks(), "jobs", HTTP_JOBS_PATH);
    }

    @Test
    public void testGetReturnsAResponseThatHasACurrentUserLink() {
        final APIV1RootResponse APIV1RootResponse = createRootResourceAndGetResponse();
        assertHasKeyWithValue(
                APIV1RootResponse.getLinks(),
                "current-user",
                HTTP_USERS_PATH + "/current");
    }

    @Test
    public void testGetReturnsAResponseThatHasASpecsLink() {
        final APIV1RootResponse APIV1RootResponse = createRootResourceAndGetResponse();
        assertHasKeyWithValue(APIV1RootResponse.getLinks(), "specs", HTTP_SPECS_PATH);
    }
}
