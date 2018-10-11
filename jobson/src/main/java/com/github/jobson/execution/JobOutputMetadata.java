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
package com.github.jobson.execution;

import java.util.Map;
import java.util.Optional;

public final class JobOutputMetadata {

    private final Optional<String> mimeType;
    private final Optional<String> name;
    private final Optional<String> description;
    private final Map<String, String> metadata;


    public JobOutputMetadata(
            Optional<String> mimeType,
            Optional<String> name,
            Optional<String> description,
            Map<String, String> metadata) {
        this.mimeType = mimeType;
        this.name = name;
        this.description = description;
        this.metadata = metadata;
    }


    public Optional<String> getMimeType() {
        return mimeType;
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
}
