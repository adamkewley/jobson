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

import com.github.jobson.commands.generators.GenerateRequestCommand;
import com.github.jobson.commands.generators.GenerateSpecsCommand;
import io.dropwizard.cli.Command;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Hierarchical command for generating various Jobson components.
 */
public final class GenerateCommand extends HierarchicalCommand {

    private static SortedMap<String, Command> generateSubCommands() {
        final SortedMap<String, Command> commands = new TreeMap<>();
        final GenerateRequestCommand generateRequestCommand = new GenerateRequestCommand();
        commands.put(generateRequestCommand.getName(), generateRequestCommand);
        final GenerateSpecsCommand generateSpecsCommand = new GenerateSpecsCommand();
        commands.put(generateSpecsCommand.getName(), generateSpecsCommand);
        return commands;
    }

    public GenerateCommand() {
        super("generate","Generate components", generateSubCommands());
    }
}
