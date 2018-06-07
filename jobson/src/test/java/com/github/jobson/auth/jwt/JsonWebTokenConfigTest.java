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

package com.github.jobson.auth.jwt;

import com.github.jobson.TestHelpers;
import com.github.jobson.auth.AuthenticationBootstrap;
import io.dropwizard.auth.AuthFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Key;
import java.util.Base64;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

public final class JsonWebTokenConfigTest {

    @Test
    public void testCreateAuthFilterReturnsAnAuthFilter() {
        final String secretKey = TestHelpers.generateBase64SecretKey();
        final JsonWebTokenConfig jwtConfig = new JsonWebTokenConfig(secretKey);
        final AuthenticationBootstrap authBootstrap = TestHelpers.createTypicalAuthBootstrap();
        final AuthFilter ret = jwtConfig.createAuthFilter(authBootstrap);

        assertThat(ret).isNotNull();
    }

    @Test
    public void testTheReturnedAuthFilterAppliesASecurityContextToAValidRequest() throws IOException {
        final String secretKey = TestHelpers.generateBase64SecretKey();
        final String username = TestHelpers.generateRandomString();

        final JsonWebTokenConfig jwtConfig = new JsonWebTokenConfig(secretKey);
        final AuthenticationBootstrap authBootstrap = TestHelpers.createTypicalAuthBootstrap();
        final AuthFilter filter = jwtConfig.createAuthFilter(authBootstrap);

        final ContainerRequestContext ctx = mock(ContainerRequestContext.class);
        final String jwt = createJWT(secretKey, username);
        when(ctx.getHeaderString("Authorization")).thenReturn(String.format("Bearer %s", jwt));

        final ArgumentCaptor<SecurityContext> captor = ArgumentCaptor.forClass(SecurityContext.class);

        filter.filter(ctx);

        verify(ctx).setSecurityContext(captor.capture());

        final SecurityContext capture = captor.getValue();

        assertThat(capture.getUserPrincipal().getName()).isEqualTo(username);
    }

    private String createJWT(String secretKey, String username) {
        final byte[] decodedSecretKey = Base64.getDecoder().decode(secretKey);
        final Key secretKeyKey = new SecretKeySpec(decodedSecretKey, 0, decodedSecretKey.length, "HS512");
        final SignatureAlgorithm alg = SignatureAlgorithm.HS512;
        return Jwts.builder().setSubject(username).signWith(alg, secretKeyKey).compact();
    }
}