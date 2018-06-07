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

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authorizer;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An authentication filter that plucks JWT token strings from requests'
 * "Authorization:" header to authorize incoming requests.
 */
@Priority(1000)
public final class JsonWebTokenAuthFilter<P extends Principal> extends AuthFilter<String, P> {

    private static final Pattern AUTHORIZATION_HEADER_PATTERN = Pattern.compile("Bearer ([^.]+\\.[^.]+\\.[^.]+)");
    private static final String AUTHENTICATION_SCHEME_NAME = "JWT";

    private static String tryExtractJwtTokenFromAuthorizationHeader(String authorizationHeader)
            throws WebApplicationException {

        final Matcher patternMatcher = AUTHORIZATION_HEADER_PATTERN.matcher(authorizationHeader);

        final Boolean headerHasCorrectFormat = patternMatcher.matches();

        if (headerHasCorrectFormat) {
            final String jwtToken = patternMatcher.group(1);

            return jwtToken;
        } else throw new WebApplicationException("The format of the Authorization header was invalid.", 401);
    }

    private static boolean isRequestSecure(ContainerRequestContext request) {
        final SecurityContext securityContext = request.getSecurityContext();

        return securityContext != null && securityContext.isSecure();
    }

    private static <P extends Principal> SecurityContext createSecurityContext(P principal, Authorizer<P> authorizer, boolean isSecure) {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return principal;
            }

            @Override
            public boolean isUserInRole(String s) {
                return authorizer.authorize(principal, s);
            }

            @Override
            public boolean isSecure() {
                return isSecure;
            }

            @Override
            public String getAuthenticationScheme() {
                return AUTHENTICATION_SCHEME_NAME;
            }
        };
    }

    /**
     * Filter an incoming request.
     *
     * @param containerRequestContext The incoming request.
     *
     * @throws NullPointerException If containerRequestContext is null.
     * @throws WebApplicationException If an authentication error occurred.
     * @throws IOException If an internal error occurred.
     */
    @Override
    public void filter(ContainerRequestContext containerRequestContext)
            throws NullPointerException, WebApplicationException {

        final String authorizationHeaderValue = containerRequestContext.getHeaderString("Authorization");

        if (authorizationHeaderValue == null)
            throw new WebApplicationException("Authorization header was not set.", 401);

        final String jwtToken = tryExtractJwtTokenFromAuthorizationHeader(authorizationHeaderValue);

        final P principal = this.tryAuthenticateUsingJwtToken(jwtToken);

        final boolean isSecure = isRequestSecure(containerRequestContext);

        final SecurityContext securityContext =
                createSecurityContext(principal, this.authorizer, isSecure);

        containerRequestContext.setSecurityContext(securityContext);
    }

    private P tryAuthenticateUsingJwtToken(String jwtToken)
            throws WebApplicationException {
        try {
            Optional<P> possiblePrincipal = this.authenticator.authenticate(jwtToken);

            if (possiblePrincipal.isPresent()) return possiblePrincipal.get();
            else throw new WebApplicationException("Authentication failed for the supplied json web token.", 401);
        } catch (AuthenticationException ex) {
            throw new WebApplicationException("Authentication failed for the supplied json web token.", 401);
        }
    }

    public static class Builder<P extends Principal> extends AuthFilterBuilder<String, P, JsonWebTokenAuthFilter<P>> {
        public Builder() {}

        @Override
        protected JsonWebTokenAuthFilter<P> newInstance() {
            return new JsonWebTokenAuthFilter<>();
        }
    }
}
