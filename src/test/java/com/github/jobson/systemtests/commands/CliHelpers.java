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


package com.github.jobson.systemtests.commands;

import com.github.jobson.App;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class CliHelpers {

    public static int run(String... args) throws IOException, InterruptedException {
        final File pwd = Files.createTempDirectory(CliHelpers.class.getSimpleName()).toFile();
        return run(pwd, args);
    }

    public static int run(File pwd, String... args) throws IOException, InterruptedException {
        final Process process = new ProcessBuilder(getAllArgs(Arrays.asList(args)))
                .inheritIO()
                .directory(pwd)
                .start();

        return process.waitFor();
    }

    private static List<String> getAllArgs(List<String> extraArgs) {
        final List<String> processArgs = new ArrayList<>();

        processArgs.add(System.getProperty("java.home") + "/bin/java");
        processArgs.add("-classpath");
        processArgs.add(reconstructClasspath());
        processArgs.add(App.class.getCanonicalName());

        processArgs.addAll(extraArgs);

        return processArgs;
    }

    private static String reconstructClasspath() {
      return Arrays.stream(((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs())
              .map(URL::getFile)
              .collect(Collectors.joining(File.pathSeparator));
    }
}
