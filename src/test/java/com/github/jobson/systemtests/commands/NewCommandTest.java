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


package com.github.jobson.systemtests.commands;

import com.github.jobson.Constants;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public final class NewCommandTest {

    @Test
    public void testCanRunNewCommand() throws IOException, InterruptedException {
        final Path pwd = Files.createTempDirectory(NewCommandTest.class.getSimpleName());
        final int exitCode = CliHelpers.run(pwd.toFile(), "new");

        assertThat(exitCode).isEqualTo(0);
        assertThat(Files.exists(pwd.resolve(Constants.WORKSPACE_SPECS_DIRNAME)));
        assertThat(Files.exists(pwd.resolve(Constants.WORKSPACE_CONFIG_FILENAME)));
        assertThat(Files.exists(pwd.resolve(Constants.WORKSPACE_USER_FILENAME)));
        assertThat(Files.exists(pwd.resolve(Constants.WORKSPACE_JOBS_DIRNAME)));
        assertThat(Files.exists(pwd.resolve(Constants.WORKSPACE_WDS_DIRNAME)));
    }

    @Test
    public void testRunningNewCommandWithDemoProducesADemoSpec() throws IOException, InterruptedException {
        final Path pwd = Files.createTempDirectory(NewCommandTest.class.getSimpleName());
        final int exitCode = CliHelpers.run(pwd.toFile(), "new", "--demo");

        assertThat(exitCode).isEqualTo(0);
        assertThat(Files.exists(pwd.resolve(Constants.WORKSPACE_SPECS_DIRNAME).resolve(Constants.DEMO_SPEC_DIRNAME)));
    }

    @Test
    public void testRunningNewCommandWithDemoProducesASpecThatIsValid() throws IOException, InterruptedException {
        final Path pwd = Files.createTempDirectory(NewCommandTest.class.getSimpleName());
        final int exitCode = CliHelpers.run(pwd.toFile(), "new", "--demo");

        assertThat(exitCode).isEqualTo(0);

        final int validationExitCode = CliHelpers.run(pwd.toFile(), "validate", "spec", Constants.DEMO_SPEC_DIRNAME);

        assertThat(validationExitCode).isEqualTo(0);
    }
}
