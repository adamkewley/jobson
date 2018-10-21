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

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public abstract class SubprocessFactoryTest {

    protected abstract SubprocessFactory getInstance();
    protected abstract SubprocessInputImpl getInputsToProgThatWritesStdoutAndExitsWith0ExitCode() throws IOException;
    protected abstract SubprocessInputImpl getInputsToProdThatWritesToStderrAndExitsWithNonzeroExitCode() throws IOException;
    protected abstract SubprocessInputImpl getBogusInputsToProg() throws IOException;


    @Test(expected = NullPointerException.class)
    public void testCreateThrowsNPEIfInputsNull() {
        final SubprocessMonitor monitor = new NullSubprocessMonitor();
        final SubprocessFactory factory = getInstance();

        factory.create(null, monitor);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateThrowsNPEIfMonitorNull() throws IOException {
        final SubprocessInputImpl inputs = getInputsToProgThatWritesStdoutAndExitsWith0ExitCode();
        final SubprocessFactory factory = getInstance();

        factory.create(inputs, null);
    }

    @Test(expected = RuntimeException.class)
    public void testCreateThrowsRuntimeExceptionIfProcessInputIsBogus() throws IOException {
        final SubprocessInputImpl inputs = getBogusInputsToProg();
        final SubprocessMonitor monitor = new NullSubprocessMonitor();
        final SubprocessFactory factory = getInstance();

        factory.create(inputs, monitor);
    }

    @Test
    public void testCreateReturnsSubprocessInstanceWhenProvidedCorrectArguments() throws IOException {
        final SubprocessInputImpl inputs = getInputsToProgThatWritesStdoutAndExitsWith0ExitCode();
        final SubprocessMonitor monitor = new NullSubprocessMonitor();
        final SubprocessFactory factory = getInstance();

        final Subprocess subproc = factory.create(inputs, monitor);

        assertThat(subproc).isNotNull();
    }

    @Test
    public void testCreateReturnedSubprocessWaitForReturnsWithZeroExitCode() throws IOException, InterruptedException {
        final SubprocessInputImpl inputs = getInputsToProgThatWritesStdoutAndExitsWith0ExitCode();
        final WaitableSubprocessMonitor monitor = new WaitableSubprocessMonitor();
        final SubprocessFactory factory = getInstance();

        factory.create(inputs, monitor);

        final int exitCode = monitor.waitFor();

        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    public void testCreateReturnedSubprocessWaitForReturnsWithNonzeroExitCode() throws IOException, InterruptedException {
        final SubprocessInputImpl inputs = getInputsToProdThatWritesToStderrAndExitsWithNonzeroExitCode();
        final WaitableSubprocessMonitor monitor = new WaitableSubprocessMonitor();
        final SubprocessFactory factory = getInstance();

        factory.create(inputs, monitor);

        final int exitCode = monitor.waitFor();

        assertThat(exitCode).isNotEqualTo(0);
    }

    @Test
    public void testCreateSubprocessCallsMonitorToStdout() throws IOException, InterruptedException {
        final SubprocessInputImpl input = getInputsToProgThatWritesStdoutAndExitsWith0ExitCode();
        final AtomicBoolean onStdoutCalled = new AtomicBoolean(false);
        final WaitableSubprocessMonitor monitor = new WaitableSubprocessMonitor() {
            @Override
            public void onStdoutUpdate(ByteBuffer buffer, boolean closed) {
                onStdoutCalled.set(true);
            }
        };
        final SubprocessFactory factory = getInstance();

        factory.create(input, monitor);

        monitor.waitFor();

        assertThat(onStdoutCalled.get()).isTrue();
    }

    @Test
    public void testCreateSubprocessCallsMonitorToStderr() throws IOException, InterruptedException {
        final SubprocessInputImpl input = getInputsToProdThatWritesToStderrAndExitsWithNonzeroExitCode();
        final AtomicBoolean onStderrCalled = new AtomicBoolean(false);
        final WaitableSubprocessMonitor monitor = new WaitableSubprocessMonitor() {
            @Override
            public void onStderrUpdate(ByteBuffer buffer, boolean closed) {
                onStderrCalled.set(true);
            }
        };
        final SubprocessFactory factory = getInstance();

        factory.create(input, monitor);

        monitor.waitFor();

        assertThat(onStderrCalled.get()).isTrue();
    }

    @Test
    public void testCreateSubprocessCallsOnExit() throws IOException, InterruptedException {
        final SubprocessInputImpl input = getInputsToProgThatWritesStdoutAndExitsWith0ExitCode();
        final AtomicBoolean onExitCalled = new AtomicBoolean(false);
        final WaitableSubprocessMonitor monitor = new WaitableSubprocessMonitor() {
            @Override
            public void onExit(int exitCode) {
                onExitCalled.set(true);
                super.onExit(exitCode);
            }
        };

        final SubprocessFactory factory = getInstance();
        factory.create(input, monitor);

        monitor.waitFor();

        assertThat(onExitCalled.get()).isTrue();
    }

    @Test
    public void testCreateSubprocessOnExitIsCalledWithExitCodeWhenExitCodeIsZero() throws InterruptedException, IOException {
        final SubprocessInputImpl input = getInputsToProgThatWritesStdoutAndExitsWith0ExitCode();
        final AtomicInteger onExitValue = new AtomicInteger(-1);
        final WaitableSubprocessMonitor monitor = new WaitableSubprocessMonitor() {
            @Override
            public void onExit(int exitCode) {
                onExitValue.set(exitCode);
                super.onExit(exitCode);
            }
        };
        final SubprocessFactory factory = getInstance();
        factory.create(input, monitor);

        monitor.waitFor();

        assertThat(onExitValue.get()).isEqualTo(0);
    }

    @Test
    public void testCreateSubprocessOnExitIsCalledWithNonzeroExitCodeWhenExitCodeIsNonzero() throws IOException, InterruptedException {
        final SubprocessInputImpl input = getInputsToProdThatWritesToStderrAndExitsWithNonzeroExitCode();
        final AtomicInteger onExitValue = new AtomicInteger(-1);

        final WaitableSubprocessMonitor monitor = new WaitableSubprocessMonitor() {
            @Override
            public void onExit(int exitCode) {
                onExitValue.set(exitCode);
                super.onExit(exitCode);
            }
        };

        final SubprocessFactory factory = getInstance();
        factory.create(input, monitor);

        monitor.waitFor();

        assertThat(onExitValue.get()).isNotEqualTo(0);
    }
}