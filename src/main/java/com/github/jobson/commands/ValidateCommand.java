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

import com.github.jobson.commands.validators.ValidateSpecCommand;
import io.dropwizard.cli.Command;

import java.util.SortedMap;
import java.util.TreeMap;

public final class ValidateCommand extends HierarchicalCommand {

    private static SortedMap<String, Command> generateSubCommands() {
        final SortedMap<String, Command> commands = new TreeMap<>();
        final ValidateSpecCommand validateSpecCommand = new ValidateSpecCommand();
        commands.put(validateSpecCommand.getName(), validateSpecCommand);
        return commands;
    }

    public ValidateCommand() {
        super("validate", "validate components", generateSubCommands());
    }
}
