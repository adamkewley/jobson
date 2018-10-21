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

import com.github.jobson.api.http.APIUserId;
import com.github.jobson.api.persistence.JobId;
import com.github.jobson.api.persistence.JobTimestamp;
import com.github.jobson.api.specs.JobSpec;
import com.github.jobson.api.specs.inputs.JobExpectedInputId;
import com.github.jobson.api.specs.inputs.JobInput;

import java.util.List;
import java.util.Map;

public interface QueuedJob {
    JobId getId();
    List<JobTimestamp> getTimestamps();
    APIUserId getOwner();
    String getName();
    Map<JobExpectedInputId, JobInput> getInputs();
    JobSpec getSpec();
}
