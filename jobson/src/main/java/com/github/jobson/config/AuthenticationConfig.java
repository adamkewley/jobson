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

package com.github.jobson.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.jobson.auth.AuthenticationBootstrap;
import com.github.jobson.auth.basic.BasicAuthenticatorConfig;
import com.github.jobson.auth.custom.CustomAuthenticatorConfig;
import com.github.jobson.auth.guest.GuestAuthenticationConfig;
import com.github.jobson.auth.jwt.JsonWebTokenConfig;
import io.dropwizard.auth.AuthFilter;

import static com.github.jobson.Constants.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BasicAuthenticatorConfig.class, name = BASIC_AUTH_NAME),
        @JsonSubTypes.Type(value = GuestAuthenticationConfig.class, name = GUEST_AUTH_NAME),
        @JsonSubTypes.Type(value = CustomAuthenticatorConfig.class, name = CUSTOM_AUTH_NAME),
        @JsonSubTypes.Type(value = JsonWebTokenConfig.class, name = JWT_AUTH_NAME),
})
public interface AuthenticationConfig {
    AuthFilter createAuthFilter(AuthenticationBootstrap bootstrap);
}
