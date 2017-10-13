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

package com.github.jobson.dao.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jobson.api.v1.UserId;

import javax.validation.constraints.NotNull;

import static java.lang.String.format;

public final class UserCredentials {

    public static UserCredentials fromUserFileLine(String line) {
        final String fields[] = line.split(":");

        if (fields.length == 3) {
            return new UserCredentials(new UserId(fields[0]), fields[1], fields[2]);
        } else {
            throw new RuntimeException(format(
                    "Error reading a line in the users file. %s columns are present " +
                    "in '%s'. Three columns (username, auth method, and auth field) are expected.",
                    fields.length,
                    line));
        }
    }


    @JsonProperty
    @NotNull
    private UserId id;

    @JsonProperty
    @NotNull
    private String authName;

    @JsonProperty
    @NotNull
    private String authField;


    public UserCredentials(UserId id, String authName, String authField) {
        this.id = id;
        this.authName = authName;
        this.authField = authField;
    }


    public UserId getId() {
        return id;
    }

    public String getAuthName() {
        return authName;
    }

    public String getAuthField() {
        return authField;
    }


    public String toUserFileLine() {
        return id + ":" + authName + ":" + authField;
    }

    @Override
    public String toString() {
        return toUserFileLine();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserCredentials that = (UserCredentials) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (authName != null ? !authName.equals(that.authName) : that.authName != null) return false;
        return authField != null ? authField.equals(that.authField) : that.authField == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (authName != null ? authName.hashCode() : 0);
        result = 31 * result + (authField != null ? authField.hashCode() : 0);
        return result;
    }
}
