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

package com.github.jobson.systemtests.auth;

import com.github.jobson.config.ApplicationConfig;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import static com.github.jobson.Constants.HTTP_USERS_PATH;
import static com.github.jobson.HttpStatusCodes.OK;
import static com.github.jobson.systemtests.SystemTestHelpers.createStandardRuleWithTemplate;
import static com.github.jobson.systemtests.SystemTestHelpers.generateRequest;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public final class TestGuestAuth {

    @ClassRule
    public static final DropwizardAppRule<ApplicationConfig> RULE =
            createStandardRuleWithTemplate("fixtures/systemtests/auth/guest-auth-config.yml");

    @Test
    public void testBoots() {}

    @Test
    public void testCanUseApiToRequestUserId() {
        final Invocation.Builder b = generateRequest(RULE, HTTP_USERS_PATH + "/current");

        final Response response = b.get();

        assertThat(response.getStatus()).isEqualTo(OK);
    }
}
