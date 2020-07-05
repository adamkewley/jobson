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

package com.github.jobson.systemtests;

import com.github.jobson.App;
import com.github.jobson.Constants;
import com.github.jobson.TestHelpers;
import com.github.jobson.api.v1.APIJobDetails;
import com.github.jobson.config.ApplicationConfig;
import com.github.jobson.jobs.JobId;
import com.github.jobson.specs.JobSpec;
import com.github.jobson.systemtests.httpapi.TestJobSpecsAPI;
import io.dropwizard.testing.junit.DropwizardAppRule;

import javax.ws.rs.client.Invocation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static com.github.jobson.Constants.HTTP_JOBS_PATH;
import static io.dropwizard.testing.FixtureHelpers.fixture;

public final class SystemTestHelpers {

    // In user file
    public static final String SYSTEMTEST_USER = "testuser";
    public static final String SYSTEMTEST_PASSWORD = "password123";

    
    public static DropwizardAppRule<ApplicationConfig> createStandardRule() {
        return createStandardRuleWithTemplate("fixtures/systemtests/application-config-template.yml");
    }

    public static DropwizardAppRule<ApplicationConfig> createStandardRuleWithTemplate(String fixture) {
        try {
            final Path usersFilePath = Files.createTempFile(SystemTestHelpers.class.getSimpleName(), "user-file");
            final String users = fixture("fixtures/systemtests/users");
            Files.write(usersFilePath, users.getBytes());

            final Path sessionsFilePath = Files.createTempFile(SystemTestHelpers.class.getSimpleName(), "sessions-file");

            final Path jobSpecsDir = Files.createTempDirectory(TestJobSpecsAPI.class.getSimpleName());
            final List<JobSpec> specs =
                    Arrays.asList(
                            TestHelpers.YAML_MAPPER.readValue(
                                    fixture("fixtures/systemtests/jobspecs.yml"),
                                    JobSpec[].class));
            for (JobSpec spec : specs) {
                Files.createDirectory(jobSpecsDir.resolve(spec.getId().toString()));

                final String specYAML = TestHelpers.YAML_MAPPER.writeValueAsString(spec);

                Files.write(jobSpecsDir.resolve(spec.getId().toString()).resolve(Constants.SPEC_DIR_SPEC_FILENAME), specYAML.getBytes());
            }

            final String secondSpecScript = fixture("fixtures/systemtests/script.sh");
            Files.write(jobSpecsDir.resolve("second-spec").resolve("script.sh"), secondSpecScript.getBytes());

            final String eighthSpecDependency = fixture("fixtures/systemtests/eighth-spec-dependency");
            Files.write(jobSpecsDir.resolve("eighth-spec").resolve("eighth-spec-dependency"), eighthSpecDependency.getBytes());

            final Path jobDataDir = Files.createTempDirectory(SystemTestHelpers.class.getSimpleName());
            final Path workingDirsDir = Paths.get(".").toAbsolutePath().relativize(Files.createTempDirectory(SystemTestHelpers.class.getSimpleName()));

            final String resolvedAppConfigText =
                    fixture(fixture)
                            .replaceAll("\\$userFile", usersFilePath.toAbsolutePath().toString())
                            .replaceAll("\\$sessionsFile", sessionsFilePath.toAbsolutePath().toString())
                            .replaceAll("\\$jobSpecDir", jobSpecsDir.toAbsolutePath().toString())
                            .replaceAll("\\$jobDataDir", jobDataDir.toAbsolutePath().toString())
                            .replaceAll("\\$workingDirsDir", workingDirsDir.toString());


            final Path resolvedAppConfigPath = Files.createTempFile(TestJobSpecsAPI.class.getSimpleName(), "config");

            Files.write(resolvedAppConfigPath, resolvedAppConfigText.getBytes());

            return new DropwizardAppRule<>(App.class, resolvedAppConfigPath.toString());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Invocation.Builder generateRequest(DropwizardAppRule<ApplicationConfig> rule, String absPath) {
        final String path = String.format("http://localhost:%d" + absPath, rule.getLocalPort());
        return rule.client().target(path).request();
    }

    public static Invocation.Builder generateAuthenticatedRequest(DropwizardAppRule<ApplicationConfig> rule, String absPath) {
        final Invocation.Builder ret = generateRequest(rule, absPath);
        authenticate(ret);
        return ret;
    }

    public static void authenticate(Invocation.Builder builder) {
        authenticate(builder, SYSTEMTEST_USER, SYSTEMTEST_PASSWORD);
    }

    public static void authenticate(Invocation.Builder builder, String username, String password) {
        final String headerVal = username + ":" + password;
        final byte[] headerValByes = headerVal.getBytes();
        final String headerBase64 = Base64.getEncoder().encodeToString(headerValByes);
        builder.header("Authorization", "Basic " + headerBase64);
    }

    public static void waitUntilJobTerminates(DropwizardAppRule<ApplicationConfig> rule, JobId jobId) throws InterruptedException {
        int maxAttempts = 50;
        while (maxAttempts-- > 0) {
            final APIJobDetails resp =
                    generateAuthenticatedRequest(rule, jobResourceSubpath(jobId)).get().readEntity(APIJobDetails.class);
            if (resp.latestStatus().isFinal()) {
                break;
            } else {
                Thread.sleep(50);
            }
        }

    }

    public static String jobResourceSubpath(Object subpath) {
        return HTTP_JOBS_PATH + "/" + subpath.toString();
    }
}
