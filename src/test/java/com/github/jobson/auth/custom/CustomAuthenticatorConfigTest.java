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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jobson.auth.AuthenticationBootstrap;
import com.github.jobson.dao.users.UserDAO;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.Test;

import javax.servlet.Servlet;
import java.io.IOException;

import static com.github.jobson.Helpers.readJSON;
import static com.github.jobson.TestHelpers.generateClassName;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

public final class CustomAuthenticatorConfigTest {

    private static AuthenticationBootstrap createTypicalAuthBootstrap() {
        final UserDAO userDAO = mock(UserDAO.class);
        final Server s = new Server(0);
        final Servlet se = new ServletContainer();
        final JerseyEnvironment env = new JerseyEnvironment(new JerseyContainerHolder(se), new DropwizardResourceConfig());

        return new AuthenticationBootstrap(env, userDAO);
    }


    @Test(expected = NullPointerException.class)
    public void testCreateAuthFilterThrowsIfClassNameIsNull() {
        final CustomAuthenticatorConfig config =
                new CustomAuthenticatorConfig(null);
        config.createAuthFilter(createTypicalAuthBootstrap());
    }

    @Test(expected = RuntimeException.class)
    public void testCreateAuthFilterIfClassNameDoesNotExistOnClassPath() {
        final CustomAuthenticatorConfig config =
                new CustomAuthenticatorConfig(generateClassName());
        config.createAuthFilter(createTypicalAuthBootstrap());
    }

    @Test(expected = RuntimeException.class)
    public void testCreateAuthFilterIfClassDoesNotDeriveFromAuthenticationConfig() {
        final CustomAuthenticatorConfig config =
                new CustomAuthenticatorConfig(Object.class.getName());
        config.createAuthFilter(createTypicalAuthBootstrap());
    }

    @Test
    public void testCreateAuthFilterDoesNotThrowIfClassDoesDeriveFromAuthenticationConfig() {
        final CustomAuthenticatorConfig config =
                new CustomAuthenticatorConfig(NullCustomAuthConfig.class.getName());
        config.createAuthFilter(createTypicalAuthBootstrap());
    }
}