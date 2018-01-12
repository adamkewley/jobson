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

import com.github.jobson.Helpers;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import static com.github.jobson.TestHelpers.generateUserDetails;
import static com.github.jobson.TestHelpers.generateUserId;
import static org.assertj.core.api.Assertions.assertThat;

public final class FilesystemUserDAOTest {

    @Test(expected = NullPointerException.class)
    public void testCtorThrowsIfUsersFileIsNull() throws FileNotFoundException {
        new FilesystemUserDAO(null);
    }

    @Test(expected = FileNotFoundException.class)
    public void testCtorThrowsIfUsersFileDoesNotExist() throws FileNotFoundException {
        final File f = new File("does-not-exist-" + Helpers.generateRandomBase36String(5));
        new FilesystemUserDAO(f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCtorThrowsIfUsersFileIsNotAFile() throws IOException {
        final File dir = Files.createTempDirectory(FilesystemUserDAOTest.class.getSimpleName()).toFile();
        new FilesystemUserDAO(dir);
    }


    @Test(expected = NullPointerException.class)
    public void testGetUserDetailsByIdThrowsNPEIfArgsNull() throws IOException {
        final FilesystemUserDAO dao = new FilesystemUserDAO(tmpFile());

        dao.getUserCredentialsById(null);
    }

    private File tmpFile() throws IOException {
        return Files.createTempFile(FilesystemUserDAOTest.class.getSimpleName(), "").toFile();
    }

    @Test
    public void testGetUserDetailsByIdReturnsEmptyOptionalForABogusUserId() throws IOException {
        final FilesystemUserDAO dao = new FilesystemUserDAO(tmpFile());

        assertThat(dao.getUserCredentialsById(generateUserId())).isEmpty();
    }

    @Test
    public void testGetUserDetailsByIdReturnsUserDetailsFromFilesystem() throws IOException {
        final UserCredentials userCredentials = generateUserDetails();

        final File usersFile = tmpFile();
        Files.write(usersFile.toPath(), userCredentials.toUserFileLine().getBytes());

        final FilesystemUserDAO dao = new FilesystemUserDAO(usersFile);

        final Optional<UserCredentials> maybeCredentials = dao.getUserCredentialsById(userCredentials.getId());

        assertThat(maybeCredentials).isNotEmpty();
        assertThat(maybeCredentials.get()).isEqualTo(userCredentials);
    }

    @Test
    public void testGetUserDetailsByIdReturnsUserDetailsEvenWhenFileContainsBlankLines() throws IOException {
        final UserCredentials userCredentials = generateUserDetails();

        final File usersFile = tmpFile();
        final String userLine = userCredentials.toUserFileLine();
        final String fileContent = "#somecomment\n\n\n" + userLine + "\n#anothercomment\n";
        Files.write(usersFile.toPath(), fileContent.getBytes());

        final FilesystemUserDAO dao = new FilesystemUserDAO(usersFile);

        final Optional<UserCredentials> maybeCredentials = dao.getUserCredentialsById(userCredentials.getId());

        assertThat(maybeCredentials).isNotEmpty();
        assertThat(maybeCredentials.get()).isEqualTo(userCredentials);
    }
}