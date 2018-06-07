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

import com.github.jobson.dao.jobs.WritingJobDAO;

import java.util.Optional;

public final class JobOutputPersister implements JobOutputResultVisitorT<Optional<String>> {

    private final JobId jobId;
    private final WritingJobDAO jobDAO;

    public JobOutputPersister(JobId jobId, WritingJobDAO jobDAO) {
        this.jobId = jobId;
        this.jobDAO = jobDAO;
    }

    @Override
    public Optional<String> visit(MissingOutput missingOutput) {
        return missingOutput.isRequired() ?
                Optional.of(String.format("%s (expected at: %s) was missing", missingOutput.getId(), missingOutput.getExpectedLocation())) :
                Optional.empty();
    }

    @Override
    public Optional<String> visit(JobOutput jobOutput) {
        this.jobDAO.persistOutput(this.jobId, jobOutput);
        return Optional.empty();
    }
}
