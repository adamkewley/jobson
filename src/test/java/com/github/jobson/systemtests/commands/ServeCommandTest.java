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

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public final class ServeCommandTest {

    @Test
    @Ignore
    public void testRunningServeWithStandardConfigBootsOkAndOpensRelevantPorts() throws IOException, InterruptedException {
        final Path pwd = Files.createTempDirectory(ServeCommandTest.class.getSimpleName());

        final int newCommandExitCode = CliHelpers.run(pwd.toFile(), "new");
        assertThat(newCommandExitCode).isEqualTo(0);

        final List<String> args = CliHelpers.getAllArgs(Arrays.asList("serve", "config.yml"));

        final Process serverProcess = new ProcessBuilder(args)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .directory(pwd.toFile())
                .start();

        try {
            // Let server spin up
            Thread.sleep(2000);

            assertThat(isTcpPortAvailable(8080)).isFalse();
        } finally {
            System.out.flush();
            System.err.flush();
            serverProcess.destroy();
        }
    }

    public static boolean isTcpPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket()) {
            // setReuseAddress(false) is required only on OSX,
            // otherwise the code will not work correctly on that platform
            serverSocket.setReuseAddress(false);
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), port), 1);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
