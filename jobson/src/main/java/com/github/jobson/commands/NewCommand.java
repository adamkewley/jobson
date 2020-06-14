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

import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.Charset;

import static com.github.jobson.Constants.*;
import static com.github.jobson.Helpers.openResourceFile;
import static com.google.common.io.ByteStreams.copy;
import static java.nio.file.Files.newOutputStream;
import static org.apache.commons.io.IOUtils.toInputStream;

public final class NewCommand extends Command {

    private static final String DEMO_ARG_NAME = "--demo";

    public NewCommand() {
        super("new", "generate a new jobson deployment in the current working directory");
    }


    @Override
    public void configure(Subparser subparser) {
        subparser.addArgument(DEMO_ARG_NAME)
                .dest(DEMO_ARG_NAME)
                .action(Arguments.storeConst())
                .setConst(true)
                .setDefault(false)
                .help("Generate application with a demo spec");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
        try {
            final Path configPath = Paths.get(WORKSPACE_CONFIG_FILENAME);
            tryWriteFile(configPath, openResourceFile("config-template.yml"));

            final Path usersPath = Paths.get(WORKSPACE_USER_FILENAME);
            tryWriteFile(usersPath, toInputStream("", Charset.forName("UTF-8")));

            final Path specDir = Paths.get(WORKSPACE_SPECS_DIRNAME);
            tryCreateDir(specDir);

            if (namespace.getBoolean(DEMO_ARG_NAME)) {
                tryWriteDemoSpec(specDir);
            }

            final Path jobsDir = Paths.get(WORKSPACE_JOBS_DIRNAME);
            tryCreateDir(jobsDir);

            final Path wdsDir = Paths.get(WORKSPACE_WDS_DIRNAME);
            tryCreateDir(wdsDir);

            System.out.println("Deployment created. Remember to add users (`user add`, `user passwd`), specs (`generate spec`), and boot the server (`serve`)");
            System.exit(0);
        } catch (IOException ex) {
            System.err.println(ex.toString());
            System.err.println(
                    "Error creating jobson files/directories. Do you have file permissions? " +
                    "Could some of the files already exist (this app won't overwrite files)?");
            System.exit(1);
        }
    }

    private void tryWriteFile(Path path, InputStream data) throws IOException {
        if (!path.toFile().exists()) {
            System.err.println("create    " + path);
            copy(data, newOutputStream(path));
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

    private void tryWriteDemoSpec(Path specsDir) throws IOException {
        final Path demoDirPath = specsDir.resolve(DEMO_SPEC_DIRNAME);
        tryCreateDir(demoDirPath);

        tryWriteFile(demoDirPath.resolve(SPEC_DIR_SPEC_FILENAME), openResourceFile("demo-spec.yml"));
        tryWriteFile(demoDirPath.resolve("demo-script.sh"), openResourceFile("demo-script.sh"));
        tryWriteFile(demoDirPath.resolve("demo-dependency"), openResourceFile("demo-dependency"));
    }
}
