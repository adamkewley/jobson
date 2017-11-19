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

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.PrincipalImpl;
import io.dropwizard.auth.basic.BasicCredentials;

import java.security.Principal;
import java.util.Optional;

public final class SpecificUsernamePwAuthenticator implements Authenticator<BasicCredentials, Principal> {

    private final String username;
    private final String password;


    public SpecificUsernamePwAuthenticator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public Optional<Principal> authenticate(BasicCredentials basicCredentials) throws AuthenticationException {
        if (basicCredentials.getUsername().equals(username) &&
                basicCredentials.getPassword().equals(password)) {
            return Optional.of(new PrincipalImpl(username));
        } else return Optional.empty();
    }
}
