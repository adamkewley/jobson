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

package com.github.jobson.auth.custom;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.jobson.auth.AuthenticationBootstrap;
import com.github.jobson.config.AuthenticationConfig;
import io.dropwizard.auth.AuthFilter;

import java.security.Principal;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, visible = true) // I know this is annoying.
public final class CustomAuthConfigWithProperties implements AuthenticationConfig {

    @JsonProperty
    private String prop1;

    @JsonProperty
    private String prop2;


    public String getProp1() {
        return prop1;
    }

    public String getProp2() {
        return prop2;
    }

    @Override
    public AuthFilter<?, Principal> createAuthFilter(AuthenticationBootstrap bootstrap) {
        return null;
    }
}
