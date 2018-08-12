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

package com.github.jobson.specs;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.Objects;

public final class JobDependencyConfiguration {

    @JsonProperty
    @NotNull
    private RawTemplateString source;

    @JsonProperty
    @NotNull
    private RawTemplateString target;

    @JsonProperty
    private boolean softLink = false;


    /**
     * @deprecated Used by JSON deserializer
     */
    public JobDependencyConfiguration() {}

    public JobDependencyConfiguration(String source, String target) {
        this.source = new RawTemplateString(source);
        this.target = new RawTemplateString(target);
    }

    public JobDependencyConfiguration(String source, String target, boolean softLink) {
        this.source = new RawTemplateString(source);
        this.target = new RawTemplateString(target);
        this.softLink = softLink;
    }


    public RawTemplateString getSource() {
        return source;
    }

    public RawTemplateString getTarget() {
        return target;
    }

    public boolean isSoftLink() {
        return softLink;
    }

    public JobDependencyConfiguration withSourceResolvedRelativeTo(Path p) {
        return new JobDependencyConfiguration(p.resolve(source.getValue()).toString(), target.getValue());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobDependencyConfiguration that = (JobDependencyConfiguration) o;
        return softLink == that.softLink &&
                Objects.equals(source, that.source) &&
                Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {

        return Objects.hash(source, target, softLink);
    }
}
