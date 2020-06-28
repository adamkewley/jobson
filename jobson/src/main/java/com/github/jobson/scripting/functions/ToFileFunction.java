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
import com.github.jobson.scripting.FreeFunction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;

public final class ToFileFunction implements FreeFunction {

    private final Path workingDir;


    public ToFileFunction(Path workingDir) {
        this.workingDir = workingDir;
    }


    @Override
    public Object call(Object... args) {
        if (args.length != 1) {
            throw new RuntimeException(format("toFile called with %s args (expects 1)", args.length));
        }

        if (args[0] instanceof String) {
            return this.call((String)args[0]);
        }

        if (args[0] instanceof FileInput) {
            return this.call((FileInput)args[0]);
        }

        throw new RuntimeException(format(
                "asFile called with %s, should be called with a string (try using toJSON?)",
                args[0].getClass().getSimpleName()));
    }

    private Object call(String fileContent) {
        try {
            final Path path = Files.createTempFile(workingDir, "request", "");
            Files.write(path, fileContent.getBytes());
            return path.toAbsolutePath().toString();
        } catch (IOException ex) {
            throw new RuntimeException("Could not create an input file.", ex);
        }
    }

    private Object call(FileInput fi) {
        try {
            Path p = workingDir.resolve(fi.getFilename());
            if (Files.exists(p)) {
                // The "ideal" path (e.g. unmodified in the working dir) already exists. This *could* be because
                // the client provided the same-named file multiple times through the client (perfectly allowed,
                // because there might be several expectedInputs)
                //
                // The client *may* be reliant on prefixes/suffixes in the provided filename to do something useful
                // (e.g. searching for file starting with DATA_, or ending with .gz). We still want the file to be in
                // the working dir (because clients might treat that in a special way w.r.t. cleaning up afterwards etc.)
                // so our only option is to create a randomly-named directory and put the file in there. This way, its
                // name is unchanged (good) *and* it's in the working directory (also good)
                //
                // The randomly-generated dirname should try and include the desired filename, so that it's easier for
                // a developer to guess what's in the dir.
                final Path tmpdir = Files.createTempDirectory(workingDir, fi.getFilename());
                p = tmpdir.resolve(fi.getFilename());
            }

            Files.write(p, fi.getData());
            return p.toAbsolutePath().toString();
        } catch (IOException ex) {
            throw new RuntimeException("Could not create an input file.", ex);
        }
    }
}
