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

package com.github.jobson.dao.jobs;

import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.jobson.jobinputs.JobExpectedInputId;
import com.github.jobson.jobs.JobId;
import com.github.jobson.jobs.JobStatus;
import com.github.jobson.specs.JobOutputId;
import com.github.jobson.specs.JobSpec;
import com.github.jobson.utils.BinaryData;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ReadonlyJobDAO {

    Optional<JobDetails> getJobDetailsById(JobId jobId);
    Optional<JobSpec> getSpecJobWasSubmittedAgainst(JobId jobId);

    List<JobDetails> getJobs(int pageSize, int page);
    List<JobDetails> getJobs(int pageSize, int page, String query);

    boolean jobExists(JobId jobId);

    boolean hasStdout(JobId jobId);
    Optional<BinaryData> getStdout(JobId jobId);

    boolean hasStderr(JobId jobId);
    Optional<BinaryData> getStderr(JobId jobId);

    Set<JobId> getJobsWithStatus(JobStatus status);

    boolean hasOutput(JobId jobId, JobOutputId outputId);
    Optional<BinaryData> getOutput(JobId jobId, JobOutputId outputId);

    List<JobOutputDetails> getJobOutputs(JobId jobId);

    boolean hasJobInputs(JobId jobId);
    Optional<Map<JobExpectedInputId, JsonNode>> getJobInputs(JobId jobId);

    Map<String, HealthCheck> getHealthChecks();
}
