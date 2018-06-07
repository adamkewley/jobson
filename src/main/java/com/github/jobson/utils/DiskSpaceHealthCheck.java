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


package com.github.jobson.utils;

import com.codahale.metrics.health.HealthCheck;

import java.io.File;

import static java.lang.String.format;

public final class DiskSpaceHealthCheck extends HealthCheck {

    private final File f;
    private final long minSpaceThreshold;

    public DiskSpaceHealthCheck(File f, long minSpaceThreshold) {
        this.f = f;
        this.minSpaceThreshold = minSpaceThreshold;
    }

    @Override
    protected Result check() throws Exception {
        final long usableSpace = f.getUsableSpace();
        if (usableSpace < minSpaceThreshold) {
            return Result.unhealthy(format(
                    "Out of disk space: only %s bytes remain in %s. The threshold for this warning is: %s",
                    usableSpace,
                    f.getAbsolutePath(),
                    minSpaceThreshold));
        } else {
            return Result.healthy(usableSpace + ": bytes remain at " + f.getAbsolutePath());
        }
    }
}
