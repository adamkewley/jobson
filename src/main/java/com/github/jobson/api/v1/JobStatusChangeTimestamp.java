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

package com.github.jobson.api.v1;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jobson.Constants;
import com.github.jobson.Helpers;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;
import java.util.Optional;

@ApiModel(description = "A job status change")
public final class JobStatusChangeTimestamp {

    public static JobStatusChangeTimestamp now(JobStatus jobStatus, String message) {
        return new JobStatusChangeTimestamp(jobStatus, Helpers.now(), Optional.of(message));
    }

    public static JobStatusChangeTimestamp now(JobStatus jobStatus) {
        return new JobStatusChangeTimestamp(jobStatus, Helpers.now(), Optional.empty());
    }



    @ApiModelProperty(value = "The new status of the job")
    @JsonProperty
    private JobStatus status;

    @ApiModelProperty(value = "When the status change occurred")
    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.ALI_VISIBLE_TIMESTAMPS_FORMAT, timezone = Constants.ALI_VISIBLE_TIMESTAMPS_TIMEZONE)
    private Date time;

    @ApiModelProperty(value = "(optional) A message associated with the status change")
    @JsonProperty
    private Optional<String> message = Optional.empty();



    /**
     * @deprecated Used by JSON deserializer
     */
    public JobStatusChangeTimestamp() {}


    public JobStatusChangeTimestamp(JobStatus status, Date time, Optional<String> message) {
        this.status = status;
        this.time = time;
        this.message = message;
    }



    public JobStatus getStatus() {
        return status;
    }

    public Date getTime() {
        return time;
    }

    public Optional<String> getMessage() {
        return message;
    }
}
