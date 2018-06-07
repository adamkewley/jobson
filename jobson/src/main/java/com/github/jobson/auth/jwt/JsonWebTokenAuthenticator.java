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

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.PrincipalImpl;
import io.jsonwebtoken.*;

import java.security.Key;
import java.security.Principal;
import java.util.Objects;
import java.util.Optional;

/**
 * A stateless JwtAuthenticator that verifies only that the Jwt was not
 * manipulated in any way (the hashes are valid) and that the Jwt has
 * not expired. Provided it was not manipulated, the user principal within
 * the JWT is produced with no round-trips to any persistence layer.
 */
public final class JsonWebTokenAuthenticator implements Authenticator<String, Principal> {

    public static String createJwtToken(SignatureAlgorithm alg, Key secretKey, Principal principal) {
        return Jwts.builder().setSubject(principal.getName()).signWith(alg, secretKey).compact();
    }

    private final Key secretKey;
    private final SignatureAlgorithm algorithm;

    /**
     * Create an instance of StatelessJwtAuthenticator that uses the
     * provided secret key and signature algorithm to verify incoming
     * JWTs.
     *
     * @param secretKey The secret key to use to verify incoming JWTs.
     * @param algorithm The algorithm to use to verify incoming JWTs.
     * @throws NullPointerException If secretKey or algorithm are null.
     */
    public JsonWebTokenAuthenticator(Key secretKey, SignatureAlgorithm algorithm) throws NullPointerException {
        Objects.requireNonNull(secretKey);
        Objects.requireNonNull(algorithm);

        this.secretKey = secretKey;
        this.algorithm = algorithm;
    }

    /**
     * Authenticate a raw JWT string.
     *
     * @param s A JWT string.
     * @return An optional containing a user principal, if the JWT's contents
     * could be verified.
     * @throws NullPointerException If s is null.
     * @throws AuthenticationException If the supplied token was invalid in any way.
     */
    @Override
    public Optional<Principal> authenticate(String s) throws NullPointerException, AuthenticationException {
        Objects.requireNonNull(s);

        try {
            final Jws<Claims> claims = Jwts.parser().setSigningKey(this.secretKey).parseClaimsJws(s);

            final String username = claims.getBody().getSubject();

            final Principal principal = new PrincipalImpl(username);

            return Optional.of(principal);

        } catch (MalformedJwtException ex) {
            throw new AuthenticationException("The provided json web token was malformed.", ex);
        } catch (SignatureException ex) {
            throw new AuthenticationException("The provided json web token failed signature validation tests.", ex);
        }
    }

    /**
     * Create a JWT token from a userPrincipal principal.
     *
     * @param principal The principal to create a JWT for.
     * @return A JWT token string.
     * @throws NullPointerException If principal is null.
     */
    public String createJwtToken(Principal principal) {
        return createJwtToken(this.algorithm, this.secretKey, principal);
    }
}
