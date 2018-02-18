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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jobson.auth.AuthenticationBootstrap;
import com.github.jobson.auth.PermitAllAuthorizer;
import com.github.jobson.config.AuthenticationConfig;
import io.dropwizard.auth.AuthFilter;
import io.jsonwebtoken.SignatureAlgorithm;
import org.hibernate.validator.constraints.NotEmpty;

import javax.crypto.spec.SecretKeySpec;
import javax.validation.constraints.NotNull;
import java.security.Key;
import java.security.Principal;
import java.util.Base64;

public final class JsonWebTokenConfig implements AuthenticationConfig {

    @JsonProperty
    @NotNull
    @NotEmpty
    private String secretKey;  // Base64 string

    /**
     * @deprecated Used by JSON deserializer.
     */
    public JsonWebTokenConfig() {}

    public JsonWebTokenConfig(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return SignatureAlgorithm.HS512;
    }

    @Override
    public AuthFilter<?, Principal> createAuthFilter(AuthenticationBootstrap bootstrap) {
        final byte[] decodedSecretKey = Base64.getDecoder().decode(secretKey);
        final Key secretKeyKey = new SecretKeySpec(decodedSecretKey, 0, decodedSecretKey.length, this.getSignatureAlgorithm().toString());

        return new JsonWebTokenAuthFilter.Builder<>()
                .setAuthenticator(new JsonWebTokenAuthenticator(secretKeyKey, this.getSignatureAlgorithm()))
                .setAuthorizer(new PermitAllAuthorizer())
                .buildAuthFilter();
    }
}
