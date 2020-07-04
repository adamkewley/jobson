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

import com.github.jobson.jobinputs.file.FileInput;
import com.github.jobson.jobinputs.filearray.FileArrayInput;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.Map;
import java.util.Scanner;

public final class TemplateStringEvaluator {

    public static String evaluate(String templateString, Map<String, Object> environment) {
        final Scanner s = new Scanner(templateString);
        // TODO: This is a hack that fails if a string literal contains "}"
        // TODO: It's because i couldn't be bothered nesting grammars.
        s.useDelimiter("((?!\\\\)\\$\\{)|(})");

        StringBuilder ret = new StringBuilder();
        boolean isInsideExpr = templateString.startsWith("${");
        while(s.hasNext()) {
            final String str = s.next();

            if (isInsideExpr) {
                final JsLikeExpressionLexer lexer = new JsLikeExpressionLexer(CharStreams.fromString(str));
                final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
                final JsLikeExpressionParser parser = new JsLikeExpressionParser(tokenStream);


                final TemplateStringEvaluatorVisitor visitor = new TemplateStringEvaluatorVisitor(environment);

                final Object evaluationOutput = parser.expression().accept(visitor);

                // The evaluation of a template string element (e.g. ${someExpr}) can either yield
                //
                // - A "normal" java object (e.g. String, number). Could be the result of a function (e.g. `toString`)
                //   or literal (e.g. `"str"`). Should be left untouched and dumped into the output
                //
                // - A `JobInput`. Probably the result of a lookup operation (e.g. `${inputs.fileInput}`). Should be
                //   coerced to its final output value.
                //
                // For `JobInput`s specifically, the coercions into a string are as follows:
                //
                // numeric (f32, f64, i32, i64) -> string (e.g. `1.34 -> "1.34")
                // string-like (select, sql, string, stringarray) -> string (e.g. ["a", "b"] -> "a,b")
                // file -> path to a temporary file
                String coercedValue;
                if (evaluationOutput instanceof FileInput) {
                    coercedValue = ((FreeFunction)environment.get("toFile")).call(evaluationOutput).toString();
                } else if (evaluationOutput instanceof FileArrayInput) {
                    coercedValue = ((FreeFunction)environment.get("toDir")).call(evaluationOutput).toString();
                } else {
                    coercedValue = evaluationOutput.toString();
                }

                ret.append(coercedValue);
            } else {
                ret.append(str);
            }

            isInsideExpr = !isInsideExpr;
        }

        return ret.toString();
    }
}
