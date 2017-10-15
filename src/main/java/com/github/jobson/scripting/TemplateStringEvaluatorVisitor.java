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
import static java.lang.String.join;
import static java.util.stream.Collectors.toList;

public final class TemplateStringEvaluatorVisitor extends TemplateStringBaseVisitor<Object> {

    private final Map<String, Object> environment;


    public TemplateStringEvaluatorVisitor(Map<String, Object> environment) {
        this.environment = environment;
    }


    @Override
    public Object visitTemplateString(TemplateStringParser.TemplateStringContext ctx) {
        return join("", ctx.templateStringComponent().stream()
                .map(component -> component.accept(this).toString())
                .collect(toList()));
    }

    @Override
    public Object visitExpressionComponent(TemplateStringParser.ExpressionComponentContext ctx) {
        return ctx.expression().accept(this);
    }

    @Override
    public Object visitStringComponent(TemplateStringParser.StringComponentContext ctx) {
        return ctx.getText();
    }

    @Override
    public Object visitStringLiteralExpression(TemplateStringParser.StringLiteralExpressionContext ctx) {
        // Strip and unescape it
        final String str = ctx.getText();
        return str.substring(1, str.length() - 1).replace("\\\"", "\"");
    }

    @Override
    public Object visitMemberDotExpression(TemplateStringParser.MemberDotExpressionContext ctx) {
        final Object lhs = ctx.expression().accept(this);
        return evaluateObjectMember(lhs, ctx.Identifier().getText());
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
    public Object visitMemberIndexExpression(TemplateStringParser.MemberIndexExpressionContext ctx) {
        final Object lhs = ctx.expression(0).accept(this);
        final String rhs = ctx.expression(1).accept(this).toString();
        return evaluateObjectMember(lhs, rhs);
    }

    @Override
    public Object visitFunctionCallExpression(TemplateStringParser.FunctionCallExpressionContext ctx) {
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

    private List<Object> evaluateFunctionArgs(TemplateStringParser.FunctionArgsContext ctx) {
        return ctx.expression().stream().map(expr -> expr.accept(this)).collect(toList());
    }

    @Override
    public Object visitIdentifierExpression(TemplateStringParser.IdentifierExpressionContext ctx) {
        return environment.get(ctx.getText());
    }

    @Override
    public Object visitFunctionArgs(TemplateStringParser.FunctionArgsContext ctx) {
        throw new RuntimeException("Call to function args context - this shouldn't happen");
    }
}
