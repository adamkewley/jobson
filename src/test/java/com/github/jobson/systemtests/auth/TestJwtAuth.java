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

import com.github.jobson.TestHelpers;
import com.github.jobson.api.v1.APIUserDetails;
import com.github.jobson.auth.jwt.JsonWebTokenAuthenticator;
import com.github.jobson.auth.jwt.JsonWebTokenConfig;
import com.github.jobson.config.ApplicationConfig;
import io.dropwizard.auth.PrincipalImpl;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.ClassRule;
import org.junit.Test;

import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.security.Key;
import java.security.Principal;
import java.util.Base64;

import static com.github.jobson.Constants.HTTP_USERS_PATH;
import static com.github.jobson.HttpStatusCodes.OK;
import static com.github.jobson.HttpStatusCodes.UNAUTHORIZED;
import static com.github.jobson.systemtests.SystemTestHelpers.createStandardRuleWithTemplate;
import static com.github.jobson.systemtests.SystemTestHelpers.generateRequest;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public final class TestJwtAuth {

    @ClassRule
    public static final DropwizardAppRule<ApplicationConfig> RULE =
            createStandardRuleWithTemplate("fixtures/systemtests/auth/jwt-auth-config.yml");


    @Test
    public void testApplicationBoots() {
        // Triggers @ClassRule
    }

    @Test
    public void testAPIRequiresAuthentication() {
        final Invocation.Builder b = generateRequest(RULE, HTTP_USERS_PATH + "/current");
        final Response response = b.get();

        assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    public void testClientCanAuthenticateWithAJwt() {
        final JsonWebTokenConfig jwtConfigInFixture =
                (JsonWebTokenConfig)RULE.getConfiguration().getAuthenticationConfiguration();

        final String secretKeyBase64 = jwtConfigInFixture.getSecretKey();
        final byte[] secretKeyData = Base64.getDecoder().decode(secretKeyBase64);
        final SignatureAlgorithm alg = jwtConfigInFixture.getSignatureAlgorithm();
        final Key secretKey = new SecretKeySpec(secretKeyData, 0, secretKeyData.length, alg.toString());

        final String username = TestHelpers.generateRandomString();
        final Principal userPrincipal = new PrincipalImpl(username);
        final String jwt = JsonWebTokenAuthenticator.createJwtToken(alg, secretKey, userPrincipal);

        final Invocation.Builder b = generateRequest(RULE, HTTP_USERS_PATH + "/current");
        b.header("Authorization", "Bearer " + jwt);

        final Response response = b.get();
        assertThat(response.getStatus()).isEqualTo(OK);

        final APIUserDetails parsedResponse = response.readEntity(APIUserDetails.class);
        assertThat(parsedResponse.getId().toString()).isEqualTo(username);
    }
}
