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

import com.github.jobson.api.v1.UserId;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.util.Objects.requireNonNull;

public final class FilesystemUserDAO implements UserDAO {

    private final File usersFile;


    public FilesystemUserDAO(File usersFile) throws FileNotFoundException {
        requireNonNull(usersFile);
        if (!usersFile.exists())
            throw new FileNotFoundException(usersFile.toString() + ": No such file (users file)");
        if (!usersFile.isFile())
            throw new IllegalArgumentException(usersFile.toString() + ": Is not a file (users file)");

        this.usersFile = usersFile;
    }

    @Override
    public Optional<UserCredentials> getUserCredentialsById(UserId id) {
        requireNonNull(id);

        try {
            return readUserCredentials()
                    .filter(c -> c.getId().equals(id))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<UserCredentials> readUserCredentials() throws IOException {
        return Files.readAllLines(usersFile.toPath())
                .stream()
                .map(String::trim)
                .filter(l -> l.length() > 0 && !l.startsWith("#")) // comment line
                .map(UserCredentials::fromUserFileLine);
    }

    @Override
    public boolean addNewUser(UserId id, String authName, String authField) {
        try {
            final boolean userExists =
                    readUserCredentials().anyMatch(c -> c.getId().equals(id));

            if (!userExists) {
                final UserCredentials c = new UserCredentials(id, authName, authField);
                final String s = c.toUserFileLine() + System.lineSeparator();
                Files.write(usersFile.toPath(), s.getBytes(), APPEND);
                return true;
            } else return false;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean updateUserAuth(UserId id, String authName, String authField) {
        try {
            final List<UserCredentials> allCredentials = readUserCredentials().collect(Collectors.toList());

            final boolean userExists = allCredentials.stream().anyMatch(c -> c.getId().equals(id));

            if (userExists) {
                // TODO: Clean up
                final UserCredentials newUserCredentials = new UserCredentials(id, authName, authField);
                final Stream<UserCredentials> otherCredentials = allCredentials.stream().filter(c -> !c.getId().equals(id));
                final Stream<UserCredentials> upd = Stream.concat(otherCredentials, Stream.of(newUserCredentials));
                final String fileContent = String.join("", upd.map(c -> c + System.lineSeparator()).collect(Collectors.toList()));
                Files.write(usersFile.toPath(), fileContent.getBytes());
                return true;
            } else return false;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
