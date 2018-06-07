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

import com.github.jobson.specs.JobOutputId;

public final class MissingOutput implements JobOutputResult {

    private JobOutputId id;
    private boolean required;
    private String expectedLocation;

    public MissingOutput(JobOutputId id, boolean required, String expectedLocation) {
        this.id = id;
        this.required = required;
        this.expectedLocation = expectedLocation;
    }

    public JobOutputId getId() {
        return id;
    }

    public boolean isRequired() {
        return required;
    }

    public String getExpectedLocation() {
        return expectedLocation;
    }

    @Override
    public <T> T accept(JobOutputResultVisitorT<T> visitor) {
        return visitor.visit(this);
    }
}
