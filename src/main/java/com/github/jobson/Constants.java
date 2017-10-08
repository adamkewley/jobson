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

package com.github.jobson;

public final class Constants {

    public static final String WORKSPACE_CONFIG_FILENAME = "config.yml";
    public static final String WORKSPACE_USER_FILENAME = "users";
    public static final String WORKSPACE_SPECS_DIRNAME = "specs";
    public static final String WORKSPACE_WDS_DIRNAME = "wds";
    public static final String WORKSPACE_JOBS_DIRNAME = "jobs";

    public static final String JOB_DIR_STDOUT_FILENAME = "stdout";
    public static final String JOB_DIR_STDERR_FILENAME = "stderr";
    public static final String JOB_DIR_JOB_DETAILS_FILENAME = "request.json";
    public static final String JOB_DIR_JOB_SPEC_FILENAME = "spec.json";
    public static final String JOB_DIR_OUTPUTS_DIRNAME = "outputs";
    public static final String JOB_DIR_OUTPUTS_FILENAME = "outputs.json";

    public static final String SPEC_DIR_SPEC_FILENAME = "spec.yml";


    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_RUNNING_JOBS = 10;
    public static final int DELAY_BEFORE_FORCIBLY_KILLING_JOBS = 10;
    public static final int MAX_JOB_ID_GENERATION_ATTEMPTS = 100;
    public static final int STDIO_BUFFER_LEN_IN_BYTES = 256;


    public static final String API_VISIBLE_TIMESTAMPS_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSS'Z'";
    public static final int JOB_TIMESTAMP_RESOLUTION_IN_MILLISECONDS = 1;
    public static final String API_VISIBLE_TIMESTAMPS_TIMEZONE = "UTC";
    public static final String WEBSOCKET_JOB_EVENTS_PATH = "/v1/jobs/events";

    public static final String BASIC_AUTH_NAME = "basic";
    public static final String GUEST_AUTH_NAME = "disabled";
}
