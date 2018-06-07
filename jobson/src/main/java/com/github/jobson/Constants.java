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
    public static final String JOB_DIR_JOB_INPUTS_FILENAME = "inputs.json";
    public static final String JOB_DIR_OUTPUTS_DIRNAME = "outputs";
    public static final String JOB_DIR_OUTPUTS_FILENAME = "outputs.json";

    public static final String SPEC_DIR_SPEC_FILENAME = "spec.yml";


    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_CONCURRENT_JOBS = 10;
    public static final int DELAY_BEFORE_FORCIBLY_KILLING_JOBS_IN_MILLISECONDS = 10000;
    public static final int MAX_JOB_ID_GENERATION_ATTEMPTS = 100;
    public static final int STDIO_BUFFER_LEN_IN_BYTES = 256;


    public static final String API_VISIBLE_TIMESTAMPS_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSS'Z'";
    public static final int JOB_TIMESTAMP_RESOLUTION_IN_MILLISECONDS = 1;
    public static final String API_VISIBLE_TIMESTAMPS_TIMEZONE = "UTC";
    public static final boolean API_PRETTIFY_JSON_OUTPUT = true;


    public static final String WEBSOCKET_JOB_EVENTS_PATH = "/v1/jobs/events";
    public static final String WEBSOCKET_STDOUT_UPDATES_PATTERN = "/v1/jobs/(.+?)/stdout/updates";
    public static final String WEBSOCKET_STDERR_UPDATES_PATTERN = "/v1/jobs/(.+?)/stderr/updates";
    public static final long WEBSOCKET_TCP_IDLE_TIMEOUT_IN_MILLISECONDS = 1000 * 60 * 10; // 10 min.

    public static final String HTTP_ROOT = "/";
    public static final String HTTP_V1_ROOT = HTTP_ROOT + "v1/";
    public static final String HTTP_USERS_PATH = HTTP_V1_ROOT + "users";
    public static final String HTTP_SPECS_PATH = HTTP_V1_ROOT + "specs";
    public static final String HTTP_JOBS_PATH = HTTP_V1_ROOT + "jobs";


    public static final String BASIC_AUTH_NAME = "basic";
    public static final String GUEST_AUTH_NAME = "guest";
    public static final String CUSTOM_AUTH_NAME = "custom";
    public static final String JWT_AUTH_NAME = "jwt";

    public static final String DEFAULT_BASIC_AUTH_REALM = "JobsonBasicAuth";
    public static final String DEFAULT_GUEST_AUTH_REALM = "GUEST";
    public static final String DEFAULT_GUEST_USERNAME = "guest";

    public static final String DEMO_SPEC_DIRNAME = "demo";

    public static final String FILESYSTEM_JOBS_DAO_DISK_SPACE_HEALTHCHECK = "JobsDiskHasSufficientSpace";
    public static final long FILESYSTEM_JOBS_DAO_DISK_SPACE_WARNING_THRESHOLD_IN_BYTES = 500000000;  // 0.5 GB
    public static final String FILESYSTEM_SPECS_DAO_DISK_SPACE_HEALTHCHECK = "SpecsDiskHasSufficientSpace";
    public static final long FILESYSTEM_SPECS_DAO_DISK_SPACE_WARNING_THRESHOLD_IN_BYTES = 500000000;  // 0.5 GB
    public static final String JOB_MANAGER_JOB_QUEUE_OVERFLOW_HEALTHCHECK = "JobManagerJobQueue";
    public static final long JOB_MANAGER_MAX_JOB_QUEUE_OVERFLOW_THRESHOLD = 10_000;

    public static final String DEFAULT_BINARY_MIME_TYPE = "application/octet-stream";

    public static final int MAX_JOB_OUTPUT_SIZE_IN_BYTES_BEFORE_DISABLING_COMPRESSION = 52430000;  // 50 MiB
}
