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

package com.github.jobson.auth.guest;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.PrincipalImpl;

import java.security.Principal;
import java.util.Optional;

/**
 * An authenticator that always authenticates, returning a principal with the
 * ctor-supplied username.
 */
public final class GuestAuthenticator implements Authenticator<String, Principal> {

    private final Optional<Principal> p;

    public GuestAuthenticator(String guestUserName) {
        this.p = Optional.of(new PrincipalImpl(guestUserName));
    }

    @Override
    public Optional<Principal> authenticate(String ignoredString) throws AuthenticationException {
        // The redundant string is needed because the dropwizard auth doesn't like
        // nulls being passed around (even if an arg is Void).
        return p;
    }
}
