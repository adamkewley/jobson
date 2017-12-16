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

import com.github.jobson.TestHelpers;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public final class ValidateCommandTest {

    @Test
    public void testCallingValidateCommandHelpReturnsExitCode0() throws IOException, InterruptedException {
        final int exitCode = CliHelpers.run("validate", "--help");

        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    public void testCallingValidateSpecHelpCommandHelpReturnsExitCode0() throws IOException, InterruptedException {
        final int exitCode = CliHelpers.run("validate", "spec", "--help");

        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    public void testCallingInvalidValidateSubcommandWithHelpReturnsNonzeroExitCode() throws IOException, InterruptedException {
        final int exitCode = CliHelpers.run("validate", TestHelpers.generateRandomString(), "--help");

        assertThat(exitCode).isNotEqualTo(0);
    }
}
