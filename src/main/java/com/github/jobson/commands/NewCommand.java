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

package com.github.jobson.commands;

import com.github.jobson.Constants;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.jobson.Helpers.loadResourceFileAsString;

public final class NewCommand extends Command {

    public NewCommand() {
        super("new", "generate a new jobson deployment in the current working directory");
    }


    @Override
    public void configure(Subparser subparser) {}

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
        final String configTemplate;

        try {
            configTemplate = loadResourceFileAsString("config-template.yml");
        } catch (IOException ex) {
            System.err.println(ex.toString());
            System.err.println("Error loading template files. This should not happen and is not your fault - please report it");
            System.exit(1);
            return;
        }

        try {
            final Path configPath = Paths.get(Constants.WORKSPACE_CONFIG_FILENAME);
            tryWriteFile(configPath, configTemplate.getBytes());

            final Path usersPath = Paths.get(Constants.WORKSPACE_USER_FILENAME);
            tryWriteFile(usersPath, new byte[]{});

            final Path specDir = Paths.get(Constants.WORKSPACE_SPECS_DIRNAME);
            tryCreateDir(specDir);

            final Path jobsDir = Paths.get(Constants.WORKSPACE_JOBS_DIRNAME);
            tryCreateDir(jobsDir);

            final Path wdsDir = Paths.get(Constants.WORKSPACE_WDS_DIRNAME);
            tryCreateDir(wdsDir);

            System.out.println("Deployment created. Remember to add users (useradd, passwd), specs (generate), and boot the server (serve)");
            System.exit(0);
        } catch (IOException ex) {
            System.err.println(ex.toString());
            System.err.println(
                    "Error creating jobson files/directories. Do you have file permissions? " +
                    "Could some of the files already exist (this app won't overwrite files)?");
            System.exit(1);
        }
    }

    private void tryWriteFile(Path path, byte[] content) throws IOException {
        if (!path.toFile().exists()) {
            System.err.println("create    " + path);
            Files.write(path, content);
        } else {
            System.err.println("cannot create file '" + path + "': file exists: skipping");
        }
    }

    private void tryCreateDir(Path path) throws IOException {
        if (!path.toFile().exists()) {
            System.err.println("create    " + path);
            Files.createDirectory(path);
        } else {
            System.err.println("cannot create directory '" + path + "': already exists: skipping");
        }
    }
}
