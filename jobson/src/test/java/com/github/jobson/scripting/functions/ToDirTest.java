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

package com.github.jobson.scripting.functions;

import com.github.jobson.Helpers;
import com.github.jobson.TestHelpers;
import com.github.jobson.jobinputs.file.FileInput;
import com.github.jobson.jobinputs.filearray.FileArrayInput;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public final class ToDirTest {

    @Test
    public void testCanConstruct() throws IOException {
        final Path tmpdir = Files.createTempDirectory("toDirTest");
        new ToDirFunction(tmpdir);
    }

    @Test
    public void testWhenCalledWithFileArrayReturnsString() throws IOException {
        final Path tmpdir = Files.createTempDirectory("toDirTest");
        final ToDirFunction f = new ToDirFunction(tmpdir);
        final ArrayList<FileInput> fileList = new ArrayList<>();
        final FileArrayInput files = new FileArrayInput(fileList);

        final Object rv = f.call(files);

        assertThat(rv).isInstanceOf(String.class);
    }

    @Test
    public void testReturnedValueIsADirectory() throws IOException {
        final Path tmpdir = Files.createTempDirectory("toDirTest");
        final ToDirFunction f = new ToDirFunction(tmpdir);
        final ArrayList<FileInput> fileList = new ArrayList<>();
        final FileArrayInput files = new FileArrayInput(fileList);

        final File rv = new File((String)f.call(files));

        assertThat(rv.exists()).isTrue();
        assertThat(rv.isDirectory()).isTrue();
    }

    @Test
    public void testReturnedDirectoryContainsTheSuppliedFiles() throws IOException {
        final Path tmpdir = Files.createTempDirectory("toDirTest");
        final ToDirFunction f = new ToDirFunction(tmpdir);
        final ArrayList<FileInput> fileList = new ArrayList<>();
        int numFiles = TestHelpers.randomIntBetween(1, 6);
        for (int i = 0; i < numFiles; i++) {
            fileList.add(new FileInput(TestHelpers.generateAlphanumStr(), TestHelpers.generateRandomBytes()));
        }
        final FileArrayInput files = new FileArrayInput(fileList);

        final File rv = new File((String)f.call(files));
        final String[] dirents = rv.list();

        final Set<String> expectedFilenames =
                fileList.stream().map(FileInput::getFilename).collect(Collectors.toSet());

        final Set<String> foundFilenames =
                Arrays.stream(dirents).collect(Collectors.toSet());

        assertThat(expectedFilenames).isEqualTo(foundFilenames);
    }
}
