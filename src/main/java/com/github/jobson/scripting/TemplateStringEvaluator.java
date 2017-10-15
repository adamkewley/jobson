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

package com.github.jobson.scripting;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.Map;

public final class TemplateStringEvaluator {

    public static String evaluate(String templateString, Map<String, Object> environment) {
        final TemplateStringLexer lexer = new TemplateStringLexer(CharStreams.fromString(templateString));
        final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        final TemplateStringParser parser = new TemplateStringParser(tokenStream);


        final TemplateStringEvaluatorVisitor visitor = new TemplateStringEvaluatorVisitor(environment);

        return parser.templateString().accept(visitor).toString();
    }
}
