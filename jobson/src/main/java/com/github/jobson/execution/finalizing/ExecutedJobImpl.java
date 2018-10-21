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
package com.github.jobson.execution.finalizing;

import com.github.jobson.api.http.APIUserId;
import com.github.jobson.api.persistence.JobId;
import com.github.jobson.api.persistence.JobTimestamp;
import com.github.jobson.api.specs.JobSpec;
import com.github.jobson.api.specs.inputs.JobExpectedInputId;
import com.github.jobson.api.specs.inputs.JobInput;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ExecutedJobImpl implements ExecutedJob {

    private final JobId id;
    private final List<JobTimestamp> timestamps;
    private final APIUserId owner;
    private final String name;
    private final Map<JobExpectedInputId, JobInput> inputs;
    private final JobSpec spec;
    private final Path workingDir;
    private final List<String> args;
    private final int exitCode;


    public ExecutedJobImpl(
            JobId id,
            List<JobTimestamp> timestamps,
            APIUserId owner,
            String name,
            Map<JobExpectedInputId, JobInput> inputs,
            JobSpec spec,
            Path workingDir,
            List<String> args,
            int exitCode) {

        this.id = id;
        this.timestamps = timestamps;
        this.owner = owner;
        this.name = name;
        this.inputs = inputs;
        this.spec = spec;
        this.workingDir = workingDir;
        this.args = args;
        this.exitCode = exitCode;
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    @Override
    public Path getWorkingDir() {
        return workingDir;
    }

    @Override
    public JobId getId() {
        return id;
    }

    @Override
    public List<JobTimestamp> getTimestamps() {
        return timestamps;
    }

    @Override
    public APIUserId getOwner() {
        return owner;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<JobExpectedInputId, JobInput> getInputs() {
        return inputs;
    }

    @Override
    public List<String> getArgs() {
        return args;
    }

    @Override
    public JobSpec getSpec() {
        return spec;
    }
}
