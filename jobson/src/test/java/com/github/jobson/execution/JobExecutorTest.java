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
package com.github.jobson.execution;

import com.github.jobson.execution.subprocess.SubprocessFactory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class JobExecutorTest {

    protected abstract JobExecutor getInstance(JobExecutorIO ioDao, SubprocessFactory factory);

    @Test
    public void testExecuteSyncWithBadJobIdReturnsCouldNotLoadJob() {
        assertThat(false).isTrue();
    }

    @Test
    public void testExecuteSyncWithBadJobSpecReturnsPreparationFailed() {
        assertThat(false).isTrue();
    }

    @Test
    public void testExecuteSyncWithBadExecutionArgsReturnsLaunchFailed() {
        assertThat(false).isTrue();
    }

    @Test
    public void testExecuteSyncWithFailingApplicationReturnsApplicationFailed() {
        assertThat(false).isTrue();
    }

    @Test
    public void testExecuteSyncWithMissingOutputReturnsFinalizationFailed() {
        assertThat(false).isTrue();
    }

    @Test
    public void testExecuteSyncThenAbortingReturnsAborted() {
        assertThat(false).isTrue();
    }

    @Test
    public void testExecuteSyncWithGoodJobReturnsSuccess() {
        assertThat(false).isTrue();
    }

    @Test
    public void testExecuteSyncSetsJobAsExecuting() {
        assertThat(false).isTrue();
    }

    // The dependencies/staging is handled by staging impl., which is tested elsewhere

    @Test
    public void testExecuteSyncWritesStdoutToTheIODAO() {
        assertThat(false).isTrue();
    }

    @Test
    public void testExecuteSyncWritesStderrToTheIODAO() {
        assertThat(false).isTrue();
    }

    // Setting final state is handled by finalizer impl., which is tested elsewhere

    // The after-exec outputs are handled by the finalizer impl., which is tested elsewhere
}
