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
package com.github.jobson.other.system.bootup;

import com.github.jobson.api.http.APICreateJobResponse;
import com.github.jobson.api.http.APICreateJobRequest;
import com.github.jobson.api.config.ApplicationConfig;
import com.github.jobson.other.system.SystemTestHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.jobson.Constants.HTTP_JOBS_PATH;
import static com.github.jobson.other.TestHelpers.readJSONFixture;
import static com.github.jobson.other.system.SystemTestHelpers.createStandardRuleWithTemplate;
import static com.github.jobson.other.system.SystemTestHelpers.generateAuthenticatedRequest;
import static javax.ws.rs.client.Entity.json;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public final class TestAppBootsWithWdCleanupConfig {

    @ClassRule
    public static final DropwizardAppRule<ApplicationConfig> RULE =
            createStandardRuleWithTemplate("fixtures/systemtests/application-wd-cleanup-template.yml");


    @Test
    public void testCanBoot() {
        // Runs @ClassRule
    }

    @Test
    public void testWorkingDirDoesNotExistAfterExecution() throws InterruptedException {
        final APICreateJobRequest request = readJSONFixture("fixtures/systemtests/request-against-first-spec.json", APICreateJobRequest.class);

        final String createdJob =
                generateAuthenticatedRequest(RULE, HTTP_JOBS_PATH)
                        .post(json(request))
                        .readEntity(APICreateJobResponse.class)
                        .getId();

        SystemTestHelpers.waitUntilJobTerminates(RULE, createdJob);

        final Path wdPath = Paths.get(RULE.getConfiguration().getWorkingDirs().getDir()).resolve(createdJob.toString());

        assertThat(wdPath.toFile().exists()).isFalse();
    }
}
