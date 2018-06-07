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
import com.github.jobson.utils.BinaryData;

import java.util.Map;
import java.util.Optional;

public final class JobOutput implements JobOutputResult {

    private JobOutputId id;
    private BinaryData data;
    private Optional<String> name;
    private Optional<String> description;
    private Map<String, String> metadata;


    public JobOutput(
            JobOutputId id,
            BinaryData data,
            Optional<String> name,
            Optional<String> description,
            Map<String, String> metadata) {
        this.id = id;
        this.data = data;
        this.name = name;
        this.description = description;
        this.metadata = metadata;
    }


    public JobOutputId getId() {
        return id;
    }

    public BinaryData getData() {
        return data;
    }

    public Optional<String> getName() {
        return name;
    }

    public Optional<String> getDescription() {
        return description;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public <T> T accept(JobOutputResultVisitorT<T> visitor) {
        return visitor.visit(this);
    }
}
