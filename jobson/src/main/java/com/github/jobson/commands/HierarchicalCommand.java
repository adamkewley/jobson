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

import com.github.jobson.config.ApplicationConfig;
import io.dropwizard.cli.Command;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.impl.action.HelpArgumentAction;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.util.Map;
import java.util.SortedMap;

public class HierarchicalCommand extends ConfiguredCommand<ApplicationConfig> {

    private static final String COMMAND_NAME_ATTR = "subcommand";

    private final SortedMap<String, Command> commands;

    protected HierarchicalCommand(String name, String description, SortedMap<String, Command> commands) {
        super(name, description);
        this.commands = commands;

    }

    private void addCommand(Subparser subparser, Command command) {
        commands.put(command.getName(), command);
        subparser.addSubparsers().help("available commands");

        final Subparser commandSubparser =
                subparser.addSubparsers().addParser(command.getName(), false);

        command.configure(commandSubparser);

        commandSubparser.addArgument("-h", "--help")
                .action(new HelpArgumentAction())
                .help("show this help message and exit")
                .setDefault(Arguments.SUPPRESS);

        commandSubparser.description(command.getDescription())
                .setDefault(COMMAND_NAME_ATTR, command.getName())
                .defaultHelp(true);
    }

    @Override
    public void configure(Subparser subparser) {
        for (Map.Entry<String, Command> command : commands.entrySet()) {
            addCommand(subparser, command.getValue());
        }
    }

    @Override
    protected void run(Bootstrap<ApplicationConfig> bootstrap, Namespace namespace, ApplicationConfig applicationConfig) throws Exception {
        final Command command = commands.get(namespace.getString(COMMAND_NAME_ATTR));
        command.run(bootstrap, namespace);
    }
}
