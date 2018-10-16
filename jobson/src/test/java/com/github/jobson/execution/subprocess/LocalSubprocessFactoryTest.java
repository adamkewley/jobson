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
package com.github.jobson.execution.subprocess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class LocalSubprocessFactoryTest extends SubprocessFactoryTest {

    private static SubprocessInput inputWithArgs(String... args) throws IOException {
        final Path workingDir = Files.createTempDirectory(SubprocessFactoryTest.class.getName());

        return new SubprocessInput(Arrays.asList(args), workingDir);
    }

    @Override
    protected SubprocessFactory getInstance() {
        return new LocalSubprocessFactory();
    }

    @Override
    protected SubprocessInput getInputsToProgThatWritesStdoutAndExitsWith0ExitCode() throws IOException {
        return inputWithArgs("ls");
    }

    @Override
    protected SubprocessInput getInputsToProdThatWritesToStderrAndExitsWithNonzeroExitCode() throws IOException {
        return inputWithArgs("cat", "file-that-doesnt-exist");
    }

    @Override
    protected SubprocessInput getBogusInputsToProg() throws IOException {
        return inputWithArgs("this-prog-doesnt-exist", "so-args-are-ignored");
    }
}