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

package com.github.jobson.commands.generators;

import com.github.jobson.Constants;
import com.github.jobson.commands.DefaultedConfiguredCommand;
import com.github.jobson.config.ApplicationConfig;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static com.github.jobson.Helpers.loadResourceFileAsString;

public final class GenerateSpecsCommand extends DefaultedConfiguredCommand<ApplicationConfig> {

    private static String SPEC_NAMES_ARG = "specNames";


    private String specTemplate;


    public GenerateSpecsCommand() {
        super("spec", "generate a default spec in the configured specs folder");
    }



    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument(SPEC_NAMES_ARG)
                .metavar("SPEC_NAME")
                .type(String.class)
                .nargs("+")
                .help("names of the specs to generate in the server's spec folder");
    }


    @Override
    protected void run(Bootstrap<ApplicationConfig> bootstrap, Namespace namespace, ApplicationConfig applicationConfig) throws Exception {
        specTemplate = loadResourceFileAsString("spec-template.yml");

        final ArrayList<String> specNames = namespace.get(SPEC_NAMES_ARG);
        final Path specsDir = Paths.get(applicationConfig.getJobSpecConfiguration().getDir());

        if (specsDir.toFile().exists()) {
            ensureSpecsDoNotAlreadyExistIn(specsDir, specNames);
            createDefaultSpecDirs(specsDir, specNames);
        } else {
            System.err.println(specsDir + ": No such directory");
            System.exit(1);
        }
    }

    private void ensureSpecsDoNotAlreadyExistIn(Path specsDir, ArrayList<String> specNames) {
        for (String specName : specNames) {
            if (specsDir.resolve(specName).toFile().exists()) {
                System.err.println("cannot create spec '" + specName + "': already exists");
                System.exit(1);
            }
        }
    }

    private void createDefaultSpecDirs(Path specsDir, ArrayList<String> specNames) {
        for (String specName : specNames)
            createDefaultSpecDir(specsDir, specName);
    }

    private void createDefaultSpecDir(Path specsDir, String specName) {
        final Path specDir = specsDir.resolve(specName);

        try {
            System.err.println("create    " + specDir);
            Files.createDirectory(specDir);

            final Path specFilePath = specDir.resolve(Constants.SPEC_DIR_SPEC_FILENAME);

            System.err.println("create    " + specFilePath);

            Files.write(specFilePath, specTemplate.getBytes());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
