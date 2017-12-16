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

package com.github.jobson.commands.users;

import com.github.jobson.api.v1.UserId;
import com.github.jobson.auth.basic.BasicAuthenticator;
import com.github.jobson.commands.DefaultedConfiguredCommand;
import com.github.jobson.config.ApplicationConfig;
import com.github.jobson.dao.users.FilesystemUserDAO;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.io.File;

import static com.github.jobson.Constants.BASIC_AUTH_NAME;
import static com.github.jobson.Helpers.generateRandomBase36String;
import static java.lang.String.format;

public final class UseraddCommand extends DefaultedConfiguredCommand<ApplicationConfig> {

    private static String LOGIN_ARG = "LOGIN";
    private static String PASSWORD_ARG = "-p";

    public UseraddCommand() {
        super("add", "create a new user with a random password. Assumes basic auth.");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument(LOGIN_ARG)
                .metavar(LOGIN_ARG)
                .type(String.class)
                .help("new user's login");

        subparser.addArgument(PASSWORD_ARG, "--password")
                .dest(PASSWORD_ARG)
                .type(String.class)
                .help("the user's password");
    }

    @Override
    protected void run(Bootstrap<ApplicationConfig> bootstrap, Namespace namespace, ApplicationConfig applicationConfig) throws Exception {
        final UserId login = new UserId(namespace.get(LOGIN_ARG));
        final File userFile = new File(applicationConfig.getUsersConfiguration().getFile());
        final FilesystemUserDAO dao = new FilesystemUserDAO(userFile);

        final boolean userExists = dao.getUserCredentialsById(login).isPresent();

        if (!userExists) {
            addNewUser(namespace, dao, login);
        } else {
            System.err.println(format("user '%s' already exists, you can set this user's password with `passwd`.", login));
            System.exit(1);
        }
    }

    private void addNewUser(Namespace namespace, FilesystemUserDAO dao, UserId login) {
        final String password = namespace.getString(PASSWORD_ARG) == null ?
                generateRandomBase36String(30) :
                namespace.getString(PASSWORD_ARG);

        final boolean userAdded =
                dao.addNewUser(login, BASIC_AUTH_NAME, BasicAuthenticator.createAuthField(password));

        if (userAdded) {
            System.exit(0);
        } else {
            System.err.println("encountered an error adding a new user (this shouldn't happen)");
            System.exit(1);
        }
    }
}
