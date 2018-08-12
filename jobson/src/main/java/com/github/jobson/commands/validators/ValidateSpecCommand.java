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

package com.github.jobson.commands.validators;

import com.github.jobson.commands.DefaultedConfiguredCommand;
import com.github.jobson.config.ApplicationConfig;
import com.github.jobson.specs.ExecutionConfiguration;
import com.github.jobson.specs.JobDependencyConfiguration;
import com.github.jobson.specs.JobSpec;
import com.github.jobson.specs.JobSpecId;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.github.jobson.Constants.SPEC_DIR_SPEC_FILENAME;
import static com.github.jobson.Helpers.readYAML;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public final class ValidateSpecCommand extends DefaultedConfiguredCommand<ApplicationConfig> {

    private static final String JOB_SPEC_IDS_ARGNAME = "jobSpecIds";


    public ValidateSpecCommand() {
        super("spec", "validate a job spec");
    }


    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument(JOB_SPEC_IDS_ARGNAME)
                .metavar("JOB_SPEC_IDS")
                .type(String.class)
                .nargs("+")
                .help("IDs of the job specs to validate");
    }

    @Override
    protected void run(Bootstrap<ApplicationConfig> bootstrap, Namespace namespace, ApplicationConfig applicationConfig) throws Exception {
        final Path jobSpecsDir = Paths.get(applicationConfig.getJobSpecConfiguration().getDir());

        final ArrayList<String> specIds = namespace.get(JOB_SPEC_IDS_ARGNAME);

        final Map<JobSpecId, List<String>> allErrors =
                specIds.stream()
                        .map(specId -> getSpecErrors(jobSpecsDir, specId))
                        .filter(entry -> entry.getValue().size() > 0)
                        .collect(toMap(e -> e.getKey(), e -> e.getValue()));

        if (allErrors.size() > 0) {
            allErrors.forEach(this::printErrors);
            System.exit(1);
        } else System.exit(0);
    }

    private Map.Entry<JobSpecId, List<String>> getSpecErrors(Path jobSpecsDir, String specId) {
        final JobSpecId id = new JobSpecId(specId);
        final List<String> specErrors = validate(jobSpecsDir, id);
        return new AbstractMap.SimpleEntry<>(id, specErrors);
    }

    private List<String> validate(Path jobSpecsDir, JobSpecId jobSpecId) {
        final Path jobSpecDir = jobSpecsDir.resolve(jobSpecId.toString());
        final File jobSpecDirF = jobSpecDir.toFile();
        final Path jobSpecFile = jobSpecDir.resolve(SPEC_DIR_SPEC_FILENAME);
        final File jobSpecFileF = jobSpecFile.toFile();

        if (!jobSpecDirF.exists()) {
            return singletonList(jobSpecDir + ": does not exist");
        } else if (!jobSpecDirF.isDirectory()) {
            return singletonList(jobSpecDir + ": is not a directory");
        } else if (!jobSpecFileF.exists()) {
            return singletonList(jobSpecFile + ": does not exist");
        } else {
            final String jobSpecYAMLText;
            try {
                jobSpecYAMLText = new String(Files.readAllBytes(jobSpecFile));
            } catch (IOException ex) {
                return singletonList(jobSpecFile + ": could not read: " + ex.getMessage());
            }

            final JobSpec jobSpec;
            try {
                 jobSpec = readYAML(jobSpecYAMLText, JobSpec.class);
            } catch (IOException ex) {
                return singletonList("Could not parse yaml: " + ex.getMessage());
            }

            return validateApplicationExecution(jobSpecDir, jobSpec.getExecution());
        }
    }

    private void printErrors(JobSpecId jobSpecId, List<String> errors) {
        System.err.println(errors.size() + " errors found in " + jobSpecId + ": ");
        errors.forEach(error -> System.err.println("    " + error));
    }

    private List<String> validateApplicationExecution(Path jobSpecDir, ExecutionConfiguration execConfig) {
        final ArrayList<String> ret = new ArrayList<>();

        if (execConfig.getDependencies().isPresent()) {
            final List<JobDependencyConfiguration> dependencies = execConfig.getDependencies().get();
            final List<String> dependencyErrors =
                    dependencies.stream().flatMap(dep -> validateJobDependency(jobSpecDir, dep)).collect(toList());
            ret.addAll(dependencyErrors);
        }

        return ret;
    }

    private Stream<String> validateJobDependency(Path jobSpecDir, JobDependencyConfiguration jobDependencyConfiguration) {
        // TODO: this will fail because there isn't a job to validate against.
        final Path sourcePath = jobSpecDir.resolve(jobDependencyConfiguration.getSource().getValue());

        if (sourcePath.toFile().exists()) {
            return Stream.empty();
        } else return Stream.of(sourcePath + ": dependency does not exist");
    }
}
