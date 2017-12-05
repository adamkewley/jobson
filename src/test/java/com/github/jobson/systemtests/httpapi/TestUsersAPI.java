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

import com.github.jobson.api.v1.APIUserDetails;
import com.github.jobson.config.ApplicationConfig;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;

import static com.github.jobson.Constants.HTTP_USERS_PATH;
import static com.github.jobson.systemtests.SystemTestHelpers.*;
import static org.assertj.core.api.Assertions.assertThat;

public final class TestUsersAPI {

    @ClassRule
    public static final DropwizardAppRule<ApplicationConfig> RULE = createStandardRule();

    @Test
    public void testGetCurrentUserRespondsWithCurrentUser() throws IOException {
        final APIUserDetails APIUserDetails =
                generateAuthenticatedRequest(RULE, HTTP_USERS_PATH + "/current")
                .get()
                .readEntity(APIUserDetails.class);

        assertThat(APIUserDetails.getId().toString()).isEqualTo(SYSTEMTEST_USER);
    }
}
