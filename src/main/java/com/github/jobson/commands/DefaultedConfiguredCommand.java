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

package com.github.jobson.commands;

import io.dropwizard.Configuration;
import io.dropwizard.cli.ConfiguredCommand;
import net.sourceforge.argparse4j.inf.Subparser;

import static com.github.jobson.Constants.WORKSPACE_CONFIG_FILENAME;

/**
 * By default, Dropwizard's ConfiguredCommand will create a default instance
 * of an application configuration if a user does not provide one. Instead,
 * what I want is for it to default to a convention.
 */
public abstract class DefaultedConfiguredCommand<T extends Configuration> extends ConfiguredCommand<T> {

    protected DefaultedConfiguredCommand(String name, String description) {
        super(name, description);
    }

    @Override
    public void configure(Subparser subparser) {
        subparser.setDefault("file", WORKSPACE_CONFIG_FILENAME);

        subparser.addArgument("file")
                .nargs("?")
                .setDefault(WORKSPACE_CONFIG_FILENAME)
                .help("application configuration file");
    }
}
