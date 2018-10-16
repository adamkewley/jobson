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
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class WaitableSubprocessMonitor implements SubprocessMonitor {

    private Semaphore s = new Semaphore(-1);
    private int exitCode = -1;

    @Override
    public void onStdoutUpdate(ByteBuffer buf, boolean closed) {
        if (!closed) {
            buf.clear();
        }
    }

    @Override
    public void onStderrUpdate(ByteBuffer buf, boolean closed) {
        if (!closed) {
            buf.clear();
        }
    }

    @Override
    public void onExit(int exitCode) {
        s.release(1_073_741_824);  // TODO: should *permanantly* be open once exited.
        this.exitCode = exitCode;
    }

    public int waitFor() throws InterruptedException {
        s.acquire();
        return exitCode;
    }

    public boolean waitFor(long timeout, TimeUnit timeUnit) throws InterruptedException {
        if (s == null) {
            return true;
        } else {
            return s.tryAcquire(timeout, timeUnit);
        }
    }
}
