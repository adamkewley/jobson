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

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class MockSubprocessFactory implements SubprocessFactory {

    private final Map<String, MockApplication> mockApplications;

    public MockSubprocessFactory(Map<String, MockApplication> mockApplications) {
        Objects.requireNonNull(mockApplications);

        this.mockApplications = mockApplications;
    }

    @Override
    public Subprocess create(SubprocessInput input, SubprocessMonitor monitor) {
        final String appName = input.getArgs().get(0);

        if (mockApplications.containsKey(appName)) {
            final MockApplication mockApplications = this.mockApplications.get(appName);
            return create(input, mockApplications, monitor);
        } else {
            throw new RuntimeException(appName + ": no such (mock) application");
        }
    }

    private Subprocess create(SubprocessInput input, MockApplication mockApplication, SubprocessMonitor monitor) {
        final BlockingQueue<ProcessSignals> signalQueue = new ArrayBlockingQueue<>(10);

        new Thread(() -> {
            monitor.onStdoutUpdate(ByteBuffer.wrap(mockApplication.getStdout()), false);
            monitor.onStderrUpdate(ByteBuffer.wrap(mockApplication.getStderr()), false);
            monitor.onStdoutUpdate(ByteBuffer.wrap(new byte[] {}), true);
            monitor.onStderrUpdate(ByteBuffer.wrap(new byte[] {}), true);
            if (!signalQueue.isEmpty()) {
                monitor.onExit(1);
            } else {
                monitor.onExit(mockApplication.getExitCode());
            }
        }).start();

        return new MockSubprocess(signalQueue);
    }
}
