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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

import static com.github.jobson.Helpers.setOf;

@Schema(description = "The status of a job")
public enum JobStatus {

    @JsonProperty("submitted")
    SUBMITTED,

    @JsonProperty("running")
    RUNNING,

    @JsonProperty("aborted")
    ABORTED,

    @JsonProperty("fatal-error")
    FATAL_ERROR,

    @JsonProperty("finished")
    FINISHED;


    public static JobStatus fromExitCode(int exitCode) {
        switch (exitCode) {
            case 0: return FINISHED;
            case 130: return ABORTED; // CTRL+C
            case 143: return ABORTED; // SIGTERM
            default: return FATAL_ERROR;
        }
    }

    public static Set<JobStatus> getAbortableStatuses() {
        return setOf(SUBMITTED, RUNNING);
    }


    public boolean isAbortable() {
        return getAbortableStatuses().contains(this);
    }

    public boolean isFinal() {
        return !(this.equals(SUBMITTED) || this.equals(RUNNING));
    }

    public int toExitCode() {
        switch (this) {
            case ABORTED:
                return 112;
            case FATAL_ERROR:
                return 1;
            case FINISHED:
                return 0;
            default:
                return 1;
        }
    }
}
