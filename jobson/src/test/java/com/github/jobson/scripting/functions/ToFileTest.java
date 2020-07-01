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

import com.github.jobson.TestHelpers;
import com.github.jobson.jobinputs.file.FileInput;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public final class ToFileTest {
    @Test
    public void testWhenCalledWithStringPutsStringContentIntoTempFileInDirAndReturnsPathToFile() throws IOException {
        final Path tmpdir = Files.createTempDirectory("toFileTest");
        final ToFileFunction f = new ToFileFunction(tmpdir);
        final String content = TestHelpers.generateAlphanumStr(1024);

        final Object rv = f.call(content);

        assertThat(rv instanceof String).isTrue();

        final Path p = Paths.get((String)rv);

        assertThat(Files.exists(p)).isTrue();
        assertThat(Files.readAllBytes(p)).isEqualTo(content.getBytes());
        assertThat(p.isAbsolute()).isTrue();
        assertThat(p.startsWith(tmpdir)).isTrue();
    }

    @Test
    public void testWhenCalledWithFileInputPutsDataIntoTempFileAndReturnsPathToFile() throws IOException {
        final Path tmpdir = Files.createTempDirectory("toFileTest");
        final ToFileFunction f = new ToFileFunction(tmpdir);
        final byte[] data = TestHelpers.generateRandomBytes();
        final FileInput fi = new FileInput("not-handled-by-this-test", data);

        final Object rv = f.call(fi);

        assertThat(rv instanceof String).isTrue();

        final Path p = Paths.get((String)rv);

        assertThat(Files.exists(p)).isTrue();
        assertThat(Files.readAllBytes(p)).isEqualTo(data);
        assertThat(p.isAbsolute()).isTrue();
        assertThat(p.startsWith(tmpdir)).isTrue();
    }
}
