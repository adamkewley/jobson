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

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.jobson.utils.BinaryData;
import io.reactivex.Observer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.github.jobson.Constants.STDIO_BUFFER_LEN_IN_BYTES;
import static java.lang.System.arraycopy;
import static java.nio.file.FileVisitResult.CONTINUE;

public final class Helpers {

    private static final ObjectMapper JSON_MAPPER =
            new ObjectMapper().registerModule(
                    new Jdk8Module()).enable(SerializationFeature.INDENT_OUTPUT);

    private static final ObjectMapper YAML_MAPPER =
            new ObjectMapper(new YAMLFactory()).registerModule(new Jdk8Module());

    private static final Logger log = LoggerFactory.getLogger(Helpers.class);

    private static final Random rng = new Random();
    
    private static final char[] base36Characters = new char[] {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
            'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
            'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    };
    private static final int numBase36Characters = base36Characters.length;



    public static String generateRandomBase36String(int len) {
        final char[] ret = new char[len];

        for (int i = 0; i < len; i++) {
            final int idx = randomIntBetween(0, numBase36Characters);
            ret[i] = base36Characters[idx];
        }

        return new String(ret);
    }

    public static String capitalize(String str) {
        if (str.length() == 0) return str;
        else {
            final String firstLetter = str.substring(0, 1);
            final String rest = str.substring(1);
            return firstLetter.toUpperCase() + rest;
        }
    }

    private static int randomIntBetween(int low, int high) {
        if (low > high) throw new IllegalArgumentException();

        return rng.nextInt(high - low) + low;
    }

    public static <T, U> T randomKeyIn(Map<T, U> m) {
        Object[] keys = m.keySet().toArray();
        return (T)keys[rng.nextInt(keys.length)];
    }

    public static <T> T randomElementIn(List<T> l) {
        return l.get(randomIntBetween(0, l.size()));
    }

    public static String randomSubstring(String s, int len) {
        if (s.length() > len) {
            final int r = randomIntBetween(0, s.length() - len);
            return s.substring(r, r + len);
        } else return s;
    }



    public static Stream<File> listDirectories(Path p) {
        return Arrays.stream(p.toFile().listFiles(File::isDirectory));
    }

    public static String loadResourceFileAsString(String resourceFileName) throws IOException {
        return new String(loadResourceFile(resourceFileName));
    }

    public static byte[] loadResourceFile(String resourceFileName) throws IOException {
        return IOUtils.toByteArray(openResourceFile(resourceFileName));
    }

    public static InputStream openResourceFile(String resourceFileName) {
        return Helpers.class.getClassLoader().getResourceAsStream(resourceFileName);
    }

    public static Optional<Path> tryResolve(Path p, Object s) {
        final Path sub = p.resolve(s.toString());
        if (sub.toFile().exists()) return Optional.of(sub);
        else return Optional.empty();
    }

    public static Optional<Path> tryResolve(Path p, Object... subPaths) {
        Optional<Path> ret = Optional.of(p);

        for (Object s : subPaths) {
            ret = ret.flatMap(pp -> tryResolve(pp, s.toString()));
        }

        return ret;
    }

    public static BinaryData streamBinaryData(Path p) {
        try {
            final long size = Files.size(p);
            final InputStream dataStream = Files.newInputStream(p);

            return new BinaryData(dataStream, size);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static BinaryData streamBinaryData(Path p, String mimeType) {
        try {
            final long size = Files.size(p);
            final InputStream dataStream = Files.newInputStream(p);

            return new BinaryData(dataStream, size, mimeType);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }



    public static <T> Optional<T> lastElement(List<T> els) {
        if (els.size() == 0) return Optional.empty();
        else return Optional.of(els.get(els.size() - 1));
    }

    public static <T> Stream<T> toStream(Iterable<T> iter) {
        return StreamSupport.stream(iter.spliterator(), false);
    }

    public static <T> Set<T> setOf(T... vals) {
        final Set<T> ret = new HashSet<>();
        for(T val : vals) {
            ret.add(val);
        }
        return ret;
    }



    public static <T,U> Map<T, U> merge(Map<T, U> first, Map<T, U> second) {
        final Map<T,U> ret = new HashMap<>();
        ret.putAll(first);
        ret.putAll(second);
        return ret;
    }

    public static <T, U, V> Map<V, U> mapKeys(Map<T, U> m, Function<T, V> f) {
        return m.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(f.apply(e.getKey()), e.getValue()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    public static <T, U, V> Map<T, V> mapValues(Map<T, U> m, Function<U, V> f) {
        return m.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), f.apply(e.getValue())))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    public static <T,U> Optional<U> tryGet(Map<T, U> m, T v) {
        final U ret = m.get(v);
        return ret != null ? Optional.of(ret) : Optional.empty();
    }



    public static <T> Stream<T> intersperse(Stream<T> s, T e) {
        final Iterator<T> inner = s.iterator();
        final AtomicBoolean returnE = new AtomicBoolean(false);

        final Iterator<T> it = new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return inner.hasNext();
            }

            @Override
            public T next() {
                final T ret = returnE.get() ? e : inner.next();
                returnE.set(!returnE.get());
                return ret;
            }
        };

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED),
                false);
    }

    public static <T> String join(String delimiter, Stream<T> s) {
        return intersperse(s.map(Object::toString), delimiter).reduce("", (s1, s2) -> s1 + s2);
    }

    public static String commaSeparatedList(Iterable<?> str) {
        return join(", ", toStream(str));
    }



    public static String toJSON(Object o) {
        try {
            return JSON_MAPPER.writeValueAsString(o);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static JsonNode toJSONNode(Object o) {
        return JSON_MAPPER.valueToTree(o);
    }

    public static <T> T loadJSON(Path p, Class<T> klass) throws IOException {
        final String jsonStr = new String(Files.readAllBytes(p));
        return JSON_MAPPER.readValue(jsonStr, klass);
    }

    public static <T> T loadJSON(Path p, TypeReference<T> tr) throws IOException {
        final String str = new String(Files.readAllBytes(p));
        return readJSON(str, tr);
    }

    public static <T> T readJSON(String json, Class<T> klass) throws IOException {
        return JSON_MAPPER.readValue(json, klass);
    }

    public static <T> T readJSON(String json, TypeReference<T> tr) throws IOException {
        return JSON_MAPPER.readValue(json, tr);
    }

    public static <T> T readJSON(Path p, Class<T> klass) throws IOException {
        return JSON_MAPPER.readValue(p.toFile(), klass);
    }

    public static <T> T readJSON(TreeNode t, Class<T> klass) throws IOException {
        return JSON_MAPPER.readValue(t.traverse(), klass);
    }

    public static void writeJSON(Path p, Object o) throws IOException {
        Files.write(p, toJSON(o).getBytes());
    }

    public static <T> T readYAML(String yaml, Class<T> klass) throws IOException {
        return YAML_MAPPER.readValue(yaml, klass);
    }

    public static <T> T readYAML(Path yamlPath, Class<T> klass) throws IOException {
        final String yaml = new String(Files.readAllBytes(yamlPath));
        return readYAML(yaml, klass);
    }

    public static <T> T readYAML(File f, Class<T> klass) throws IOException {
        return readYAML(f.toPath(), klass);
    }



    public static void attachTo(
            Process process,
            Observer<byte[]> stdoutObserver,
            Observer<byte[]> stderrObserver,
            Consumer<Integer> onExit) {

        // stdout
        new Thread(() -> {
            try {
                streamInto(process.getInputStream(), stdoutObserver);
            } catch (IOException e) {
                log.debug("Could not read from stdout (probably because the process died). Stopping stdout thread.");
            }
        }).start();


        // stderr
        new Thread(() -> {
            try {
                streamInto(process.getErrorStream(), stderrObserver);
            } catch (IOException e) {
                log.debug("Could not read from stderr (probably because the process died). Stopping stderr thread.");
            }
        }).start();

        // wait
        new Thread(() -> {
            try {
                final int exitCode = process.waitFor();
                onExit.accept(exitCode);
            } catch (InterruptedException e) {
                log.error("Subprocess wait thread interrupted. This shouldn't happen.");
            }
        }).start();
    }

    private static void streamInto(InputStream inputStream, Observer<byte[]> observer) throws IOException {
        byte[] bytes = new byte[STDIO_BUFFER_LEN_IN_BYTES];

        int bufLen;
        while((bufLen = inputStream.read(bytes, 0, STDIO_BUFFER_LEN_IN_BYTES)) != -1) {
            // Copy is necessary because observers might assume the buffer is
            // immutable, coming from an observable.
            byte[] outputBytes = new byte[bufLen];
            arraycopy(bytes, 0, outputBytes, 0, bufLen);
            observer.onNext(outputBytes);
        }
        observer.onComplete();
    }

    public static Date now() {
        return new Date();
    }

    public static String getMimeType(InputStream s, String fileName) throws IOException {
        final Tika t = new Tika();
        return t.detect(s, fileName);
    }

    public static void copyPath(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException
            {
                Path destinationDir = destination.resolve(source.relativize(dir));
                try {
                    Files.copy(dir, destinationDir);
                } catch (FileAlreadyExistsException e) {
                    if (!Files.isDirectory(destinationDir))
                        throw e;
                }
                return CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException
            {
                Files.copy(file, destination.resolve(source.relativize(file)));
                return CONTINUE;
            }
        });
    }
}
