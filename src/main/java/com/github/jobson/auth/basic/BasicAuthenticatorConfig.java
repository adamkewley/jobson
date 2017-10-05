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

package com.github.jobson.auth.basic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jobson.config.AuthenticationBootstrap;
import com.github.jobson.config.AuthenticationConfig;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.PermitAllAuthorizer;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

public final class BasicAuthenticatorConfig implements AuthenticationConfig {

    @JsonProperty
    private String realm = "JobsonBasicAuth";


    @Override
    public void enable(AuthenticationBootstrap bootstrap) {
        bootstrap.getEnvironment().register(new AuthDynamicFeature(
                new BasicCredentialAuthFilter.Builder<>()
                        .setAuthenticator(new BasicAuthenticator(bootstrap.getUserDAO()))
                        .setAuthorizer(new PermitAllAuthorizer())
                        .setRealm(realm)
                        .buildAuthFilter()));
        bootstrap.getEnvironment().register(RolesAllowedDynamicFeature.class);
    }
}
