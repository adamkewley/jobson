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

import com.github.jobson.Helpers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import static com.github.jobson.Helpers.commaSeparatedList;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public final class TemplateStringEvaluatorVisitor extends JsLikeExpressionBaseVisitor<Object> {

    private final Map<String, Object> environment;


    public TemplateStringEvaluatorVisitor(Map<String, Object> environment) {
        this.environment = environment;
    }


    @Override
    public Object visitStringLiteralExpression(JsLikeExpressionParser.StringLiteralExpressionContext ctx) {
        final String str = ctx.getText();
        final String strippedString = str.substring(1, str.length() - 1);
        return strippedString.replace("\\\"", "\""); // Unescape
    }

    @Override
    public Object visitMemberDotExpression(JsLikeExpressionParser.MemberDotExpressionContext ctx) {
        final Object lhs = ctx.expression().accept(this);
        return evaluateObjectMember(lhs, ctx.Identifier().getText());
    }


    @Override
    public Object visitMemberIndexExpression(JsLikeExpressionParser.MemberIndexExpressionContext ctx) {
        final Object lhs = ctx.expression(0).accept(this);
        final String rhs = ctx.expression(1).accept(this).toString();
        return evaluateObjectMember(lhs, rhs);
    }

    private Object evaluateObjectMember(Object obj, String str) {
        final Class<?> objClass = obj.getClass();

        if (Map.class.isAssignableFrom(objClass)) {
            final Map m = (Map)obj;
            if (m.containsKey(str))
                return m.get(str);
            else {
                throw new RuntimeException(format(
                        "Cannot find '%s' in object. Available fields: %s",
                        str,
                        commaSeparatedList(m.keySet())));
            }
        } else {
            for (Method m : objClass.getMethods()) {
                if (isPublic(m)) {
                    final String getterName = "get" + Helpers.capitalize(str);
                    if (m.getName().equals(getterName) && m.getParameterCount() == 0) {
                        try {
                            return m.invoke(obj);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    } else if (m.getName().equals(str)) {
                        return new ObjectMethod(m, obj);
                    }
                }
            }
        }

        throw new RuntimeException(format("Cannot find %s on %s", str, obj.getClass().getSimpleName()));
    }

    private boolean isPublic(Method m) {
        return (m.getModifiers() & Modifier.PUBLIC) != 0;
    }


    @Override
    public Object visitFunctionCallExpression(JsLikeExpressionParser.FunctionCallExpressionContext ctx) {
        final Object maybeMethod =
                ctx.expression().accept(this);

        if (maybeMethod instanceof ObjectMethod) {
            final ObjectMethod m = (ObjectMethod)maybeMethod;
            final Object[] args = evaluateFunctionArgs(ctx.functionArgs()).toArray();
            return m.call(args);
        } else if (maybeMethod instanceof FreeFunction) {
            final FreeFunction f = (FreeFunction)maybeMethod;
            final Object[] args = evaluateFunctionArgs(ctx.functionArgs()).toArray();
            return f.call(args);
        } throw new RuntimeException(ctx.expression().getText() + ": Is not a function");
    }

    private List<Object> evaluateFunctionArgs(JsLikeExpressionParser.FunctionArgsContext ctx) {
        return ctx.expression().stream().map(expr -> expr.accept(this)).collect(toList());
    }

    @Override
    public Object visitIdentifierExpression(JsLikeExpressionParser.IdentifierExpressionContext ctx) {
        final String k = ctx.getText();
        final Object maybeElement = environment.get(k);

        if (maybeElement != null) {
            return maybeElement;
        }

        final String errMsg = String.format(
                "%s: not found in template string environment (available: %s)",
                k,
                String.join(", ", environment.keySet()));

        throw new RuntimeException(errMsg);
    }

    @Override
    public Object visitFunctionArgs(JsLikeExpressionParser.FunctionArgsContext ctx) {
        throw new RuntimeException("Tried to evaluate function args directly - this shouldn't happen");
    }
}
