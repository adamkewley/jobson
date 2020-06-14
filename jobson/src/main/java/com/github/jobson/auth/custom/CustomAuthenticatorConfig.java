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

package com.github.jobson.auth.custom;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.jobson.auth.AuthenticationBootstrap;
import com.github.jobson.config.AuthenticationConfig;
import io.dropwizard.auth.AuthFilter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Principal;
import java.util.Optional;
import java.lang.reflect.InvocationTargetException;

import static com.github.jobson.Helpers.readJSON;
import static java.lang.Thread.currentThread;

/**
 * Configuration for an authentication configuration that is loaded at runtime.
 * This top-level config loads the class (or jar) at runtime. That class is then
 * instantiated with the "properties".
 *
 * Allows for configuration-defined authenticators (rather than the ones that come
 * "in the box" with the system).
 */
public final class CustomAuthenticatorConfig implements AuthenticationConfig {

    private static ClassLoader getClassLoader(Optional<String> classPath) {
        if (classPath.isPresent()) {
            final File localPath = new File(classPath.get());

            if (!localPath.exists())
                throw new RuntimeException(classPath.get() +  ": does not exist");

            try {
                final URL fileURL = localPath.toURI().toURL();
                return URLClassLoader.newInstance(new URL[] { fileURL });
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return currentThread().getContextClassLoader();
        }
    }

    private static Class<?> loadClass(ClassLoader classLoader, String className) {
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Class<AuthenticationConfig> toAuthConfigClass(Class<?> klass) {
        if (!AuthenticationConfig.class.isAssignableFrom(klass))
            throw new RuntimeException(klass.getName() + " does not implement " + AuthenticationConfig.class.getName());

        return (Class<AuthenticationConfig>)klass;
    }

    private static AuthenticationConfig loadAuthenticationConfig(
            Optional<JsonNode> properties,
            Class<AuthenticationConfig> klass) {

        try {
            return properties.isPresent() ?
                    readJSON(properties.get(), klass) :
                    klass.getDeclaredConstructor().newInstance();
        } catch (IOException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private final String className;
    private final Optional<String> classPath;
    private final Optional<JsonNode> properties;


    public CustomAuthenticatorConfig(String className) {
        this(className, Optional.empty(), Optional.empty());
    }

    public CustomAuthenticatorConfig(String className, JsonNode properties) {
        this(className, Optional.empty(), Optional.of(properties));
    }

    @JsonCreator
    public CustomAuthenticatorConfig(
            @JsonProperty("className") String className,
            @JsonProperty("classPath") Optional<String> classPath,
            @JsonProperty("properties") Optional<JsonNode> properties) {
        this.className = className;
        this.classPath = classPath;
        this.properties = properties;
    }

    @Override
    public AuthFilter<?, Principal> createAuthFilter(AuthenticationBootstrap bootstrap) {
        final ClassLoader classLoader = getClassLoader(classPath);
        final Class<?> klass = loadClass(classLoader, className);
        final Class<AuthenticationConfig> authConfigClass = toAuthConfigClass(klass);

        final AuthenticationConfig loadedConfig = loadAuthenticationConfig(properties, authConfigClass);

        return loadedConfig.createAuthFilter(bootstrap);
    }
}
