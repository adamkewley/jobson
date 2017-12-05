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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.javafaker.Faker;
import com.github.jobson.api.v1.APIJobRequest;
import com.github.jobson.commands.DefaultedConfiguredCommand;
import com.github.jobson.config.ApplicationConfig;
import com.github.jobson.jobinputs.JobExpectedInput;
import com.github.jobson.jobinputs.JobExpectedInputId;
import com.github.jobson.jobinputs.JobInput;
import com.github.jobson.specs.JobSpec;
import com.github.jobson.specs.JobSpecId;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.github.jobson.Constants.SPEC_DIR_SPEC_FILENAME;
import static com.github.jobson.Helpers.*;
import static java.util.stream.Collectors.toMap;

public final class GenerateRequestCommand extends DefaultedConfiguredCommand<ApplicationConfig> {

    private static String SPEC_NAME_ARG = "SPEC_ID";

    public GenerateRequestCommand() {
        super("request", "generate a request against a spec");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument(SPEC_NAME_ARG)
                .metavar(SPEC_NAME_ARG)
                .type(String.class)
                .help("Name of the spec to generate a request against");
    }

    @Override
    protected void run(Bootstrap<ApplicationConfig> bootstrap, Namespace namespace, ApplicationConfig applicationConfig) throws Exception {
        final String specId = namespace.get(SPEC_NAME_ARG);
        final Path specsDir = Paths.get(applicationConfig.getJobSpecConfiguration().getDir());
        final Path specFile = specsDir.resolve(specId).resolve(SPEC_DIR_SPEC_FILENAME);

        if (specFile.toFile().exists()) {
            final JobSpec jobSpec = readYAML(specFile, JobSpec.class);
            final JobSpecId jobSpecId = new JobSpecId(specId);
            final String jobName = new Faker().lorem().sentence(5);
            final Map<JobExpectedInputId, JsonNode> generatedInputs = generateInputs(jobSpec);
            final APIJobRequest jobRequest =
                    new APIJobRequest(jobSpecId, jobName, generatedInputs);

            System.out.println(toJSON(jobRequest));
            System.exit(0);
        } else {
            System.err.println(specFile + ": No such file");
            System.exit(1);
        }
    }

    private Map<JobExpectedInputId, JsonNode> generateInputs(JobSpec jobSpec) {
        return jobSpec
                .getExpectedInputs()
                .stream()
                .collect(toMap(JobExpectedInput::getId, this::generateInput));
    }

    private JsonNode generateInput(JobExpectedInput<?> expectedInput) {
        final JobInput generatedInput = expectedInput.getDefault().isPresent() ?
                expectedInput.getDefault().get() : expectedInput.generateExampleInput();
        return toJSONNode(generatedInput);
    }
}
