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
package com.github.jobson.util;

import com.github.jobson.api.http.v1.*;
import com.github.jobson.persistence.jobs.JobDetails;
import com.github.jobson.persistence.jobs.JobOutputDetails;
import com.github.jobson.persistence.specs.JobSpecSummary;
import com.github.jobson.persistence.jobs.JobId;
import com.github.jobson.persistence.jobs.JobStatus;
import com.github.jobson.persistence.jobs.JobTimestamp;
import com.github.jobson.api.specs.JobSpec;
import com.github.jobson.api.specs.JobSpecId;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class APIMappers {

    public static JobTimestamp fromAPITimestamp(APIJobTimestamp apiJobTimestamp) {
        return new JobTimestamp(fromAPIJobStatus(apiJobTimestamp.getStatus()), apiJobTimestamp.getTime(), Optional.ofNullable(apiJobTimestamp.getMessage()));
    }

    public static JobStatus fromAPIJobStatus(APIJobStatus jobStatus) {
        switch (jobStatus) {
            case ABORTED:
                return JobStatus.ABORTED;
            case RUNNING:
                return JobStatus.RUNNING;
            case FINISHED:
                return JobStatus.FINISHED;
            case SUBMITTED:
                return JobStatus.SUBMITTED;
            case FATAL_ERROR:
                return JobStatus.FATAL_ERROR;
            default:
                throw new RuntimeException(jobStatus + ": unknown job status: cannot map");
        }
    }

    public static APIJobTimestamp toAPITimestamp(JobTimestamp timestamp) {
        return new APIJobTimestamp(toAPIJobStatus(timestamp.getStatus()), timestamp.getTime(), timestamp.getMessage().orElse(null));
    }

    public static APIJobStatus toAPIJobStatus(JobStatus jobStatus) {
        switch (jobStatus) {
            case FINISHED:
                return APIJobStatus.FINISHED;
            case RUNNING:
                return APIJobStatus.RUNNING;
            case FATAL_ERROR:
                return APIJobStatus.FATAL_ERROR;
            case SUBMITTED:
                return APIJobStatus.SUBMITTED;
            case ABORTED:
                return APIJobStatus.ABORTED;
            default:
                throw new RuntimeException(jobStatus + ": unknown job status: cannot map");
        }
    }

    public static JobDetails toJobDetails(APIGetJobDetailsResponse jobDetails) {
        return new JobDetails(
                new JobId(jobDetails.getId()),
                jobDetails.getName(),
                jobDetails.getOwner(),
                jobDetails.getTimestamps().stream().map(APIMappers::fromAPITimestamp).collect(Collectors.toList()));
    }

    public static APIGetJobOutputResponse fromJobOutput(
            String outputsFolderHref,
            JobOutputDetails jobOutputDetails) {

        return new APIGetJobOutputResponse(
                jobOutputDetails.getId().toString(),
                jobOutputDetails.getSizeInBytes(),
                outputsFolderHref,
                jobOutputDetails.getMimeType(),
                jobOutputDetails.getName(),
                jobOutputDetails.getDescription(),
                jobOutputDetails.getMetadata());
    }

    public static APIGetJobSpecResponse fromJobSpec(JobSpec jobSpec) {
        return new APIGetJobSpecResponse(
                jobSpec.getId().toString(),
                jobSpec.getName(),
                jobSpec.getDescription(),
                jobSpec.getExpectedInputs());
    }

    public static APIJobSpecSummary fromJobSpecSummary(
            JobSpecSummary summary,
            Map<String, APIRestLink> restLinks) {

        return new APIJobSpecSummary(
                summary.getId().toString(),
                summary.getName(),
                summary.getDescription(),
                restLinks);
    }

    public static JobSpecSummary toJobSpecSummary(APIJobSpecSummary summary) {
        return new JobSpecSummary(
                new JobSpecId(summary.getId()), summary.getName(), summary.getDescription());
    }
}
