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
package com.github.jobson.scripting;

import com.github.jobson.api.specs.RawTemplateString;
import com.github.jobson.api.specs.inputs.JobExpectedInputId;
import com.github.jobson.execution.staging.StagedJob;
import com.github.jobson.scripting.functions.JoinFunction;
import com.github.jobson.scripting.functions.ToFileFunction;
import com.github.jobson.scripting.functions.ToJSONFunction;
import com.github.jobson.scripting.functions.ToStringFunction;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.github.jobson.util.Helpers.mapKeys;

public final class TemplateStringResolver {

    private final Map<String, Object> env;

    public TemplateStringResolver(StagedJob job, Path jobWorkingDir) {
        this.env = new HashMap<>();
        env.put("toJSON", new ToJSONFunction());
        env.put("toFile", new ToFileFunction(jobWorkingDir));
        env.put("join", new JoinFunction());
        env.put("toString", new ToStringFunction());
        env.put("request", job);
        env.put("inputs", mapKeys(job.getInputs(), JobExpectedInputId::toString));
        env.put("outputDir", jobWorkingDir.toString());
    }

    public String resolve(RawTemplateString arg) {
        return arg.tryEvaluate(env);
    }
}
