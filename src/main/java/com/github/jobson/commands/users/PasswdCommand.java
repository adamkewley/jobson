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
import static java.lang.String.format;

public final class PasswdCommand extends DefaultedConfiguredCommand<ApplicationConfig> {

    private static final String LOGIN_ARG = "LOGIN";

    public PasswdCommand() {
        super("passwd", "set a user's password");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument(LOGIN_ARG)
                .metavar(LOGIN_ARG)
                .type(String.class)
                .help("username");
    }

    @Override
    protected void run(Bootstrap<ApplicationConfig> bootstrap, Namespace namespace, ApplicationConfig applicationConfig) throws Exception {
        final UserId login = new UserId(namespace.get(LOGIN_ARG));
        final File userFile = new File(applicationConfig.getUsersConfiguration().getFile());
        final FilesystemUserDAO dao = new FilesystemUserDAO(userFile);

        final boolean userExists = dao.getUserCredentialsById(login).isPresent();

        if (userExists) {
            System.err.println(format("Changing password for %s.", login));
            System.err.print("Enter new Jobson password: ");
            System.err.flush();
            final String pw = new String(System.console().readPassword());
            System.err.print("Retype new Jobson password: ");
            System.err.flush();
            final String retry = new String(System.console().readPassword());

            if (pw.equals(retry)) {
                dao.updateUserAuth(login, BASIC_AUTH_NAME, BasicAuthenticator.createAuthField(pw));
            } else {
                System.err.println("Sorry, passwords do not match");
                System.err.println("password unchanged");
                System.exit(1);
            }
        } else {
            System.err.println(format("user '%s' does not exist", login));
            System.exit(1);
        }
    }
}
