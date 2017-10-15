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

grammar TemplateString;

templateString
    : templateStringComponent* EOF
    ;

templateStringComponent
    : '${' expression '}'              # ExpressionComponent
    | ('\\$' | ~'${' | ' ' | Number | '-' | '>' | '&' | '/' | '.')+  # StringComponent
    ;

expression
    : expression '[' expression ']'    # MemberIndexExpression
    | expression '.' Identifier        # MemberDotExpression
    | expression functionArgs          # FunctionCallExpression
    | Identifier                       # IdentifierExpression
    | StringLiteral                    # StringLiteralExpression
    ;

functionArgs
    : '(' expression (',' expression)* ')'
    ;


Number: '0'..'9'+;

Identifier: ('A'..'Z'|'a'..'z')('A'..'Z'|'a'..'z'|'0'..'9')*;
StringLiteral :   '"' ( Escape | ~('"' | '\\' ) )+ '"';
fragment Escape : '\\' ( '"' | '\\' );

WS: (' ' | '\t' | '\r' | '\n') -> skip;