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

import com.github.jobson.api.v1.UserId;
import com.github.jobson.dao.users.ReadonlyUserDAO;
import com.github.jobson.dao.users.UserCredentials;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.PrincipalImpl;
import io.dropwizard.auth.basic.BasicCredentials;

import java.security.Principal;
import java.util.Optional;

import static com.github.jobson.Constants.BASIC_AUTH_NAME;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.codec.digest.Crypt.crypt;

/**
 * An authenticator that uses a UserDAO to authenticate users (as opposed to
 * an external system like OAuth2)
 */
public final class BasicAuthenticator implements Authenticator<BasicCredentials, Principal> {

    public static String createAuthField(String password) {
        return crypt(password);
    }

    private static boolean hasCorrectAuthType(UserCredentials userCredentials) {
        return userCredentials.getAuthName().equals(BASIC_AUTH_NAME);
    }

    private static boolean matchesTheCredentialsSuppliedByTheClient(UserCredentials userCredentials, BasicCredentials basicCredentials) {
        return userCredentials.getAuthField().equals(
                crypt(basicCredentials.getPassword(), userCredentials.getAuthField()));
    }


    private ReadonlyUserDAO readonlyUserDAO;


    public BasicAuthenticator(ReadonlyUserDAO readonlyUserDAO) {
        requireNonNull(readonlyUserDAO);

        this.readonlyUserDAO = readonlyUserDAO;
    }


    @Override
    public Optional<Principal> authenticate(BasicCredentials basicCredentials) throws AuthenticationException {
        final UserId id = new UserId(basicCredentials.getUsername());

        return readonlyUserDAO
                .getUserCredentialsById(id)
                .filter(BasicAuthenticator::hasCorrectAuthType)
                .filter(credentials -> matchesTheCredentialsSuppliedByTheClient(credentials, basicCredentials))
                .map(UserCredentials::getId)
                .map(UserId::toString)
                .map(PrincipalImpl::new);
    }
}
