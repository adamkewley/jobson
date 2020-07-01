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

import com.github.jobson.TestHelpers;
import com.github.jobson.jobinputs.file.FileInput;
import com.github.jobson.scripting.testclasses.ExampleObject;
import com.github.jobson.scripting.testclasses.JsonStringifier;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.jobson.Helpers.generateRandomBase36String;
import static com.github.jobson.Helpers.toJSON;
import static com.github.jobson.TestHelpers.*;
import static com.github.jobson.scripting.TemplateStringEvaluator.evaluate;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public final class TemplateStringEvaluatorTest {

    private static final Map<String, Object> EMPTY_ENVIRONMENT = new HashMap<>();


    @Test
    public void testPassingAnEmptyTemplateStringReturnsAnEmptyString() {
        final String ret = evaluate("", EMPTY_ENVIRONMENT);

        assertThat(ret).isEqualTo("");
    }

    @Test
    public void testPassingANonTemplatedStringReturnsTheSameString() {
        final String str = generateRandomBase36String(30);
        final String ret = evaluate(str, EMPTY_ENVIRONMENT);

        assertThat(ret).isEqualTo(str);
    }

    @Test
    public void testTypicalNonTemplatedString() {
        final String standardStrings[] = {
                "some_python_script.py",
                "/some/abs/path",
                "some-other/relative/path",
                "--option"
        };

        for (String str : standardStrings) {
            final String ret = evaluate(str, EMPTY_ENVIRONMENT);
            assertThat(ret).isEqualTo(str);
        }
    }

    @Test
    public void testPassingABasicTemplatedStringEvaluatesTheTemplateExpression() {
        final String str = "Hello ${\"world\"}";
        final String ret = evaluate(str, EMPTY_ENVIRONMENT);

        assertThat(ret).isEqualTo("Hello world");
    }

    @Test
    public void testPassingABasicTemplatedStringWithEnvironmentVariableEvaluatesAsExpected() {
        final String variableName = "x";
        final String variableValue = generateRandomBase36String(15);

        final String str = format("Hello ${%s}", variableName);

        final Map<String, Object> environment =
                singletonEnvironment(variableName, variableValue);

        final String ret = evaluate(str, environment);

        assertThat(ret).isEqualTo(format("Hello %s", variableValue));
    }

    private Map<String, Object> singletonEnvironment(String key, Object val) {
        final Map<String, Object> env = new HashMap<>();
        env.put(key, val);
        return env;
    }


    @Test
    public void testEscapesStringLiteralsCorrectly() {
        final String str = "Hello ${\"wo\\\"rld\"}";
        final String ret = evaluate(str, EMPTY_ENVIRONMENT);

        assertThat(ret).isEqualTo("Hello wo\"rld");
    }

    @Test
    public void testEscapesSingleQuotedStringLiteralsCorrectly() {
        final String str = "Hello ${'wo\\'rld'}";
        final String ret = evaluate(str, EMPTY_ENVIRONMENT);

        assertThat(ret).isEqualTo("Hello wo\\'rld");
    }

    @Test
    public void testMemberDotExpressionExpandsObjectMembers() {
        final String root = "x";
        final String memberName = "y";
        final String memberValue = generateAlphanumStr();

        final Map<String, Object> environment = new HashMap<>();
        environment.put(root, new ExampleObject(memberValue));

        final String templateString = format("${%s.%s}", root, memberName);

        final String ret = evaluate(templateString, environment);

        assertThat(ret).isEqualTo(memberValue);
    }

    @Test
    public void testMemberIndexExpressionExpandsObjectMembers() {
        final String root = "x";
        final String memberName = "y";
        final String memberValue = generateAlphanumStr();

        final Map<String, Object> environment = new HashMap<>();
        environment.put(root, new ExampleObject(memberValue));

        final String templateString = format("${%s[\"%s\"]}", root, memberName);

        final String ret = evaluate(templateString, environment);

        assertThat(ret).isEqualTo(memberValue);
    }

    @Test
    public void testBasicFunctionCallCallsTheFunction() {
        final String functionName = "f";
        final String suffixAddedByFunction = generateAlphanumStr();

        final FreeFunction f = new FreeFunction() {
            @Override
            public Object call(Object... args) {
                return args[0].toString() + suffixAddedByFunction;
            }
        };

        final String functionArg = generateAlphanumStr();
        final String templateString = format("${%s(\"%s\")}", functionName, functionArg);

        final Map<String, Object> environment = singletonEnvironment(functionName, f);

        final String ret = evaluate(templateString, environment);

        assertThat(ret).isEqualTo(functionArg + suffixAddedByFunction);
    }

    @Test
    public void testBasicFunctionCallCallsTheFunctionWithSingleQuotedString() {
        final String functionName = "f";
        final String suffixAddedByFunction = generateAlphanumStr();

        final FreeFunction f = new FreeFunction() {
            @Override
            public Object call(Object... args) {
                return args[0].toString() + suffixAddedByFunction;
            }
        };

        final String functionArg = generateAlphanumStr();
        final String templateString = format("${%s('%s')}", functionName, functionArg);

        final Map<String, Object> environment = singletonEnvironment(functionName, f);

        final String ret = evaluate(templateString, environment);

        assertThat(ret).isEqualTo(functionArg + suffixAddedByFunction);
    }

    @Test
    public void testMoreComplexTemplateString() {
        final String objectName = "JSON";
        final String arrayName = "arr";
        final List<String> array =
                generateRandomList(5, 20, TestHelpers::generateAlphanumStr);

        final JsonStringifier jsonStringifier = new JsonStringifier();
        final Map<String, Object> environment = singletonEnvironment(objectName, jsonStringifier);
        environment.put(arrayName, array);

        final String templateString = format("${%s.stringify(%s)}", objectName, arrayName);

        final String ret = evaluate(templateString, environment);

        final String expectedOutput = toJSON(array);
        assertThat(ret).isEqualTo(expectedOutput);
    }

    @Test
    public void testMemberDotExpressionEvaluatesMapsByGettingAMapByKey() {
        final String mapName = generateClassName();
        final String mapKey = generateClassName();
        final String mapValue = generateRandomString();
        final Map<String, Object> map = new HashMap<>();
        map.put(mapKey, mapValue);

        final Map<String, Object> environment =
                singletonEnvironment(mapName, map);

        final String templateString = format("${%s.%s}", mapName, mapKey);

        final String ret = evaluate(templateString, environment);

        assertThat(ret).isEqualTo(mapValue);
    }

    @Test
    public void testMemberIndexExpressionEvaluatesMapsByGettingAMapByKey() {
        final String mapName = generateClassName();
        final String mapKey = generateClassName();
        final String mapValue = generateRandomString();
        final Map<String, Object> map = new HashMap<>();
        map.put(mapKey, mapValue);

        final Map<String, Object> environment =
                singletonEnvironment(mapName, map);

        final String templateString = format("${%s[\"%s\"]}", mapName, mapKey);

        final String ret = evaluate(templateString, environment);

        assertThat(ret).isEqualTo(mapValue);
    }

    @Test
    public void testMemberIndexExpressionEvaluatesMapsByGettingAMapByKeyWithSingleQuotedString() {
        final String mapName = generateClassName();
        final String mapKey = generateClassName();
        final String mapValue = generateRandomString();
        final Map<String, Object> map = new HashMap<>();
        map.put(mapKey, mapValue);

        final Map<String, Object> environment =
                singletonEnvironment(mapName, map);

        final String templateString = format("${%s['%s']}", mapName, mapKey);

        final String ret = evaluate(templateString, environment);

        assertThat(ret).isEqualTo(mapValue);
    }

    @Test
    public void testPassingFileInputCallsToFileOnInput() throws IOException {
        final AtomicBoolean called = new AtomicBoolean(false);
        final FreeFunction f = new FreeFunction() {
            @Override
            public Object call(Object... args) {
                called.set(true);
                return "some-path";
            }
        };
        final FileInput fileInput = new FileInput("fname", "SGVsbG8sIHdvcmxkIQo=");
        final Map<String, Object> map = new HashMap<>();
        map.put("toFile", f);
        map.put("someFile", fileInput);

        final String ret = evaluate("${someFile}", map);

        assertThat(ret).isEqualTo("some-path");
        assertThat(called.get()).isTrue();
    }

    @Test
    public void testThrowsUsefulExceptionWhenIdentifierMissing() {
        Exception exceptionThrown = null;
        try {
            evaluate("${missingIdentifier}", new HashMap<>());
        } catch (Exception ex) {
            exceptionThrown = ex;
        }

        assertThat(exceptionThrown).isNotNull();
        assertThat(exceptionThrown.getMessage()).contains("missingIdentifier");
    }
}