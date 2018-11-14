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

import com.github.jobson.execution.subprocess.MockApplication;
import com.github.jobson.execution.subprocess.MockSubprocessFactory;
import com.github.jobson.execution.subprocess.SubprocessFactory;
import com.github.jobson.other.TestHelpers;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.github.jobson.other.TestHelpers.generateRandomBytes;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public final class LocalJobExecutorTest {

    private LocalJobExecutorConfig generateLocalJobExecutorConfig() {
        return null;
    }

    private JobExecutorIO generateJobExecutorIO() {
        return new MockJobExecutorIO(); // TODO: introspecting
    }

    private MockSubprocessFactory generateSubprocessFactory() {
        final Map<String, MockApplication> mockApplications = new HashMap<>();
        mockApplications.put("someapp", new MockApplication(generateRandomBytes(), generateRandomBytes(), 0, 1000));
        return new MockSubprocessFactory(mockApplications);  // TODO: introspecting
    }

    @Test(expected = NullPointerException.class)
    public void testCtorThrowsNPEIfAnyArgIsNull() {
        final LocalJobExecutorConfig config = generateLocalJobExecutorConfig();
        final JobExecutorIO jobExecutorIO = generateJobExecutorIO();
        final SubprocessFactory subprocessFactory = generateSubprocessFactory();
    }

    @Test
    public void testEnsureConformsToInterface() {
        assertThat(false).isTrue();
    }
}