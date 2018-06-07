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

package com.github.jobson.websockets.v1;

import com.github.jobson.jobs.JobId;
import com.github.jobson.jobs.JobManagerEvents;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.jobson.Constants.WEBSOCKET_STDOUT_UPDATES_PATTERN;

public final class StdoutUpdateSocketCreator implements WebSocketCreator {


    private static Pattern URI_PATTERN = Pattern.compile(WEBSOCKET_STDOUT_UPDATES_PATTERN);


    private static JobId extractJobId(String requestPath) {
        final Matcher pathMatcher = URI_PATTERN.matcher(requestPath);
        pathMatcher.find();
        return new JobId(pathMatcher.group(1));
    }


    private final JobManagerEvents jobManagerEvents;


    public StdoutUpdateSocketCreator(JobManagerEvents jobManagerEvents) {
        this.jobManagerEvents = jobManagerEvents;
    }


    @Override
    public Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest, ServletUpgradeResponse servletUpgradeResponse) {
        return this.jobManagerEvents
                .stdoutUpdates(extractJobId(servletUpgradeRequest.getRequestPath()))
                .map(observable -> new ObservableByteArraySocket("stdout updates", observable))
                .orElse(null);
    }
}
