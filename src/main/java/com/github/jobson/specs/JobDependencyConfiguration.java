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

public final class JobDependencyConfiguration {

    @JsonProperty
    @NotNull
    private String source;

    @JsonProperty
    @NotNull
    private String target;



    /**
     * @deprecated Used by JSON deserializer
     */
    public JobDependencyConfiguration() {}

    public JobDependencyConfiguration(String source, String target) {
        this.source = source;
        this.target = target;
    }



    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }


    public JobDependencyConfiguration withSourceResolvedRelativeTo(Path p) {
        return new JobDependencyConfiguration(p.resolve(source).toString(), target);
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobDependencyConfiguration that = (JobDependencyConfiguration) o;

        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        return target != null ? target.equals(that.target) : that.target == null;

    }

    @Override
    public int hashCode() {
        int result = source != null ? source.hashCode() : 0;
        result = 31 * result + (target != null ? target.hashCode() : 0);
        return result;
    }
}
