/*
 * Gaia CU5 SUEDE
 *
 * (c) 2005-2020 Gaia Data Processing and Analysis Consortium
 *
 *
 * CU5 SUEDE software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * CU5 SUEDE software is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this CU5 software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 *-----------------------------------------------------------------------------
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

import static com.github.jobson.Helpers.readJSON;
import static java.lang.Thread.currentThread;

public final class CustomAuthenticatorConfig implements AuthenticationConfig {

    private final AuthenticationConfig loadedConfig;


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

        final ClassLoader classLoader;
        if (classPath.isPresent()) {
            final File localPath = new File(classPath.get());

            if (!localPath.exists())
                throw new RuntimeException(classPath.get() +  ": does not exist");

            try {
                final URL fileURL = localPath.toURI().toURL();
                classLoader = URLClassLoader.newInstance(new URL[] { fileURL });
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            classLoader = currentThread().getContextClassLoader();
        }

        final Class<?> klass;
        try {
            klass = classLoader.loadClass(className);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }


        if (!AuthenticationConfig.class.isAssignableFrom(klass))
            throw new RuntimeException(klass.getName() + " does not implement " + AuthenticationConfig.class.getName());

        final Class<AuthenticationConfig> downcastedClass =
                (Class<AuthenticationConfig>)klass;

        final AuthenticationConfig loadedConfig;
        if (properties.isPresent()) {
            try {
                loadedConfig = readJSON(properties.get(), downcastedClass);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            try {
                loadedConfig = downcastedClass.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        }

        this.loadedConfig = loadedConfig;
    }


    public AuthenticationConfig getLoadedConfig() {
        return loadedConfig;
    }

    @Override
    public AuthFilter<?, Principal> createAuthFilter(AuthenticationBootstrap bootstrap) {
        return loadedConfig.createAuthFilter(bootstrap);
    }
}
