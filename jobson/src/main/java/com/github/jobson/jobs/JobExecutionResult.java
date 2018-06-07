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

package com.github.jobson.jobs;

import java.util.ArrayList;
import java.util.List;

public final class JobExecutionResult {

    public static JobExecutionResult fromExitCode(int exitCode) {
        return new JobExecutionResult(JobStatus.fromExitCode(exitCode));
    }


    private final JobStatus finalStatus;
    private final List<JobOutputResult> outputs;


    public JobExecutionResult(JobStatus finalStatus) {
        this.finalStatus = finalStatus;
        this.outputs = new ArrayList<>();
    }

    public JobExecutionResult(
            JobStatus finalStatus,
            List<JobOutputResult> outputs) {

        this.finalStatus = finalStatus;
        this.outputs = outputs;
    }


    public JobStatus getFinalStatus() {
        return finalStatus;
    }

    public List<JobOutputResult> getOutputs() {
        return outputs;
    }
}
