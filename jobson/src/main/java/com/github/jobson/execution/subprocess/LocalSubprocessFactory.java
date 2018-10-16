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

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.BiConsumer;

import static com.github.jobson.Constants.STDIO_BUFFER_LEN_IN_BYTES;

public final class LocalSubprocessFactory implements SubprocessFactory {

    private static final Logger log = Logger.getLogger(LocalSubprocessFactory.class);

    private static void attachMonitors(
            Process p,
            SubprocessMonitor monitor) {

        // stdout
        final ByteBuffer stdoutBuf = ByteBuffer.allocate(STDIO_BUFFER_LEN_IN_BYTES);
        new Thread(() -> {
            try {
                streamFifo(p.getInputStream(), stdoutBuf, monitor::onStdoutUpdate);
            } catch (Exception e) {
                log.debug("Could not read from stdout (probably because the process died). Stopping stdout thread.");
            }
        }).start();


        // stderr
        final ByteBuffer stderrBuf = ByteBuffer.allocate(STDIO_BUFFER_LEN_IN_BYTES);
        new Thread(() -> {
            try {
                streamFifo(p.getErrorStream(), stderrBuf, monitor::onStderrUpdate);

            } catch (IOException e) {
                log.debug("Could not read from stderr (probably because the process died). Stopping stderr thread.");
            }
        }).start();

        // wait (exit code)
        new Thread(() -> {
            try {
                monitor.onExit(p.waitFor());
            } catch (InterruptedException e) {
                log.error("Subprocess wait thread interrupted. This shouldn't happen.");
            }
        }).start();
    }

    private static void streamFifo(InputStream fifo, ByteBuffer buffer, BiConsumer<ByteBuffer, Boolean> updateConsumer) throws IOException {
        while(fifo.read(buffer.array(), 0, STDIO_BUFFER_LEN_IN_BYTES) != -1) {
            updateConsumer.accept(buffer, false);
        }
        updateConsumer.accept(buffer, true);
    }

    @Override
    public Subprocess create(SubprocessInput input, SubprocessMonitor monitor) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(monitor);

        final ProcessBuilder pb = new ProcessBuilder(input.getArgs());
        pb.directory(input.getWorkingDir().toFile());

        log.debug("launch subprocess: " + String.join(" ", input.getArgs()));

        final Process runningProcess;
        try {
            runningProcess = pb.start();
            log.info("launched: " + String.join(" ", input.getArgs()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        try {
            attachMonitors(runningProcess, monitor);
            return new LocalSubprocess(runningProcess);
        } catch (Exception ex) {
            runningProcess.destroyForcibly();
            throw new RuntimeException(ex);
        }
    }
}
