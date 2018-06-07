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
package com.github.jobson;

import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.jobson.TestHelpers.generateAlphanumStr;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class HelpersTest {

    @Test(expected = Throwable.class)
    public void testCopyPathThrowsIfSourceDoesNotExist() throws IOException {
        final Path bogusSource = Paths.get(generateAlphanumStr());
        final Path realDestination = Files.createTempDirectory(HelpersTest.class.getSimpleName()).resolve("destination");

        Helpers.copyPath(bogusSource, realDestination);
    }

    @Test(expected = Throwable.class)
    public void testCopyPathThrowsIfDestinationParentDoesNotExist() throws IOException {
        final Path realSource = Files.createTempFile(generateAlphanumStr(), generateAlphanumStr());
        final Path bogusDestination = Paths.get(generateAlphanumStr()).resolve("destination");

        Helpers.copyPath(realSource, bogusDestination);
    }

    @Test
    public void testCopyPathCopiesFileFromSourceToDestination() throws IOException {
        final Path source = Files.createTempFile(generateAlphanumStr(), generateAlphanumStr());
        final Path destination = Files.createTempDirectory(generateAlphanumStr()).resolve("destination");

        Helpers.copyPath(source, destination);

        assertThat(destination.toFile().exists()).isTrue();
    }

    @Test
    public void testCopyPathMaintainsExecutablePermissionsInDestination() throws IOException {
        final Path source = Files.createTempFile(generateAlphanumStr(), generateAlphanumStr());

        assertThat(source.toFile().canExecute()).isFalse();

        if (!source.toFile().setExecutable(true))
            return; // The filesystem doesn't support execute permissions
        final Path destination = Files.createTempDirectory(generateAlphanumStr()).resolve(generateAlphanumStr());

        Helpers.copyPath(source, destination);

        assertThat(destination.toFile().canExecute()).isTrue();
    }

    @Test
    public void testCopyPathCopiesDirectoryFromSourceToDestination() throws IOException {
        final Path source = Files.createTempDirectory(generateAlphanumStr());
        final Path destination = Files.createTempDirectory(generateAlphanumStr()).resolve(generateAlphanumStr());

        Helpers.copyPath(source, destination);

        assertThat(destination.toFile().exists()).isTrue();
    }

    @Test
    public void testCopyPathCopiesDirectoryRecursivelyFromSourceToDestination() throws IOException {
        final Path sourceDir = Files.createTempDirectory(generateAlphanumStr());
        final Path subdir = Files.createDirectory(sourceDir.resolve(generateAlphanumStr()));
        final Path fileInSubdir = Files.createFile(subdir.resolve(generateAlphanumStr()));

        final Path destination = Files.createTempDirectory(generateAlphanumStr()).resolve(generateAlphanumStr());

        Helpers.copyPath(sourceDir, destination);

        assertThat(destination.toFile().exists()).isTrue();
        assertThat(destination.resolve(sourceDir.relativize(subdir)).toFile().exists()).isTrue();
        assertThat(destination.resolve(sourceDir.relativize(fileInSubdir)).toFile().exists()).isTrue();
    }

    @Test
    public void testCopyPathCopiesDirectoryRecursivelyAndMaintainsExecutePermissions() throws IOException {
        final Path sourceDir = Files.createTempDirectory(generateAlphanumStr());
        final Path subdir = Files.createDirectory(sourceDir.resolve(generateAlphanumStr()));
        final Path fileInSubdir = Files.createFile(subdir.resolve(generateAlphanumStr()));

        if (!fileInSubdir.toFile().setExecutable(true))
            return; // The filesystem doesn't support execute permissions

        assertThat(fileInSubdir.toFile().canExecute()).isTrue();

        final Path destination = Files.createTempDirectory(generateAlphanumStr()).resolve(generateAlphanumStr());

        Helpers.copyPath(sourceDir, destination);

        assertThat(destination.toFile().exists()).isTrue();
        assertThat(destination.resolve(sourceDir.relativize(subdir)).toFile().exists()).isTrue();
        assertThat(destination.resolve(sourceDir.relativize(fileInSubdir)).toFile().exists()).isTrue();
        assertThat(destination.resolve(sourceDir.relativize(fileInSubdir)).toFile().canExecute()).isTrue();
    }
}