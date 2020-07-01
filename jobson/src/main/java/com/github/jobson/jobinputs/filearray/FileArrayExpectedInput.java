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

package com.github.jobson.jobinputs.filearray;

import com.github.jobson.jobinputs.JobExpectedInput;
import com.github.jobson.jobinputs.file.FileExpectedInput;
import com.github.jobson.jobinputs.file.FileInput;
import com.github.jobson.utils.ValidationError;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class FileArrayExpectedInput extends JobExpectedInput<FileArrayInput> {

    @Override
    public Class<FileArrayInput> getExpectedInputClass() {
        return FileArrayInput.class;
    }

    @Override
    public Optional<List<ValidationError>> validate(FileArrayInput input) {
        return Optional.empty();
    }

    @Override
    public FileArrayInput generateExampleInput() {
        final FileExpectedInput eg = new FileExpectedInput();
        final ArrayList<FileInput> lst = new ArrayList<>();
        lst.add(eg.generateExampleInput());
        lst.add(eg.generateExampleInput());
        return new FileArrayInput(lst);
    }
}
