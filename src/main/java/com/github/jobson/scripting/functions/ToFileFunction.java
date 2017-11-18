/*
 * Copyright (c) 2017 Adam Kewley
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.github.jobson.scripting.functions;

import com.github.jobson.scripting.FreeFunction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.String.format;

public final class ToFileFunction implements FreeFunction {

    private final Path workingDir;


    public ToFileFunction(Path workingDir) {
        this.workingDir = workingDir;
    }


    @Override
    public Object call(Object... args) {
        if (args.length != 1) {
            throw new RuntimeException(format("asFile called with %s args (expects 1)", args.length));
        } else if (!(args[0] instanceof String)) {
            throw new RuntimeException(format(
                    "asFile called with %s, should be called with a string (try using toJSON?)",
                    args[0].getClass().getSimpleName()));
        } else {
            try {
                final String fileContent = (String)args[0];
                final Path path = Files.createTempFile(workingDir, "request", "");
                Files.write(path, fileContent.getBytes());
                return path.toAbsolutePath().toString();
            } catch (IOException ex) {
                throw new RuntimeException("Could not create an input file.", ex);
            }
        }
    }
}
