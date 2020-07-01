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

import com.github.jobson.jobinputs.file.FileInput;
import com.github.jobson.jobinputs.filearray.FileArrayInput;
import com.github.jobson.scripting.FreeFunction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

public final class ToDirFunction implements FreeFunction {

    private final Path workingDir;

    public ToDirFunction(Path workingDir) {
        this.workingDir = workingDir;
    }

    @Override
    public Object call(Object... args) {
        if (args.length != 1) {
            throw new RuntimeException(format("toDir called with %s args (expects 1)", args.length));
        }

        final Object arg = args[0];

        if (!(arg instanceof FileArrayInput)) {
            throw new RuntimeException(format(
                    "toDir called with a %s object, should be called with a file[] list (e.g. ${inputs.someFileList})",
                    arg.getClass().getSimpleName()));
        }

        final FileArrayInput fai = (FileArrayInput)arg;

        // create a randomly-named dir in the working directory
        try {
            final Path dir = Files.createTempDirectory(workingDir, "toDir");

            // for each file the user supplies, there might be duplicate names. This is
            // allowed by the API because it's conceivable that a user would drag/add multiple
            // identically-named files from their computer an expect things to "just work" without
            // having to fuck around and rename their files to satisfy the software.
            //
            // This creates a problem server-side, though, because Jobson needs to write all
            // the files into a single directory to satisfy this function because downstream
            // applications may (reasonably) expect that the directory created by `toDir`
            // dumps all files in one directory with **no nesting**.
            //
            // Because of this, `toDir` has to rename duplicate files (unlike `toFile`). If
            // people whine about that then we can add a `toDirNested` or something.

            // Implementation:
            //
            // - stable-partition (groupingBy, in this case) the file list so that duplicate
            //   entries become adjacent
            //
            //  - stable partition used because the user may have "encoded" some information
            //    in their addition order (e.g. "I added the files in order of importance")
            //
            //  - the user may also have paired files from different directories so, again,
            //    ordering might be important
            //
            //  - e.g. A A.related A A.related ----> 1_A 2_A 1_A.related 2_A.related
            //
            // - If the partition contains 1 entry, there's no duplicates, so don't prefix;
            //   otherwise, do prefix

            final Map<String, List<FileInput>> sortedFiles =
                    fai.getFiles().stream().collect(Collectors.groupingBy(FileInput::getFilename));

            for (Map.Entry<String, List<FileInput>> e : sortedFiles.entrySet()) {
                final String filename = e.getKey();
                final List<FileInput> files = e.getValue();

                if (files.size() == 1) {
                    final Path filePath = dir.resolve(filename);
                    Files.write(filePath, files.get(0).getData());
                } else {
                    for (int i = 0; i < files.size(); i++) {
                        final String prefixedName = format("%s_%s", (i + 1), filename);
                        final Path filePath = dir.resolve(prefixedName);
                        Files.write(filePath, files.get(i).getData());
                    }
                }
            }
            return dir.toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
