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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jobson.Constants;
import com.github.jobson.Helpers;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;
import java.util.Optional;

@Schema(description = "Timestamp on a job")
public final class JobTimestamp {

    public static JobTimestamp now(JobStatus jobStatus, String message) {
        return new JobTimestamp(jobStatus, Helpers.now(), Optional.of(message));
    }

    public static JobTimestamp now(JobStatus jobStatus) {
        return new JobTimestamp(jobStatus, Helpers.now(), Optional.empty());
    }



    @Schema(description = "The new status of the job")
    @JsonProperty
    private JobStatus status;

    @Schema(description = "When the status change occurred")
    @JsonProperty
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = Constants.API_VISIBLE_TIMESTAMPS_FORMAT,
            timezone = Constants.API_VISIBLE_TIMESTAMPS_TIMEZONE)
    private Date time;

    @Schema(description = "(optional) A message associated with the status change")
    @JsonProperty
    private String message = null;



    /**
     * @deprecated Used by JSON deserializer
     */
    public JobTimestamp() {}


    public JobTimestamp(JobStatus status, Date time, Optional<String> message) {
        this.status = status;
        this.time = time;
        this.message = message.orElse(null);
    }



    public JobStatus getStatus() {
        return status;
    }

    public Date getTime() {
        return time;
    }

    public Optional<String> getMessage() {
        return message != null ? Optional.of(message) : Optional.empty();
    }
}
