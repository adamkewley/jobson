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

import * as React from "react";
import {StringInputEditor} from "./StringInputEditor";
import {SelectInputEditor} from "./SelectInputEditor";
import {StringArrayInputEditor} from "./StringArrayInputEditor";
import {SQLInputEditor} from "./SQLInputEditor";
import {UnknownInputTypeInputEditor} from "./UnknownInputTypeInputEditor";
import {IntegerInputEditor} from "./IntegerInputEditor";
import {Constants} from "../../Constants";
import {DecimalInputEditor} from "./DecimalInputEditor";
import {APIExpectedInput} from "../apitypes/APIExpectedInput";
import {InputEditorUpdate} from "./updates/InputEditorUpdate";
import {Component, ReactElement} from "react";

export interface InputEditorProps {
    suggestedValue: any;
    expectedInput: APIExpectedInput;
    onJobInputUpdate: (update: InputEditorUpdate) => void;
    key: string;
}


/**
 * Top-level (abstract) representation of a job input editor.
 *
 * Each editor should:
 * - Take a suggested value
 * - Produce either:
 *   - A valid input value (via user edits, coercion, etc.)
 *   - Errors
 *   - "Missing" value
 */
export class InputEditor extends Component<InputEditorProps> {

    private static expectedInputUiComponentCtors: { [dataType: string]: (props: InputEditorProps) => Component<any> } = {
        "string": props => new StringInputEditor(props),
        "select": props => new SelectInputEditor(props),
        "string[]": props => new StringArrayInputEditor(props),
        "sql": props => new SQLInputEditor(props),
        "int": props => new IntegerInputEditor({
            min: Constants.I32_MIN,
            max: Constants.I32_MAX,
            typeName: "int"
        }, props),
        "long": props => new IntegerInputEditor({
            min: Constants.I64_MIN,
            max: Constants.I64_MAX,
            typeName: "long"
        }, props),
        "float": props => new DecimalInputEditor({
            min: Constants.F32_MIN,
            max: Constants.F32_MAX,
            typeName: "float"
        }, props),
        "double": props => new DecimalInputEditor({
            min: Constants.F64_MIN,
            max: Constants.F64_MAX,
            typeName: "double"
        }, props),
    };

    public static getSupportedInputEditors(): string[] {
        return Object.keys(this.expectedInputUiComponentCtors);
    }

    public render(): ReactElement<any> {
        const unknownCtor = (props: InputEditorProps) => new UnknownInputTypeInputEditor(props);
        const inputEditor = InputEditor.expectedInputUiComponentCtors[this.props.expectedInput.type] || unknownCtor;
        const expectedInput = this.props.expectedInput;

        const editorProps = {
            suggestedValue: this.props.suggestedValue,
            expectedInput: expectedInput,
            onJobInputUpdate: this.props.onJobInputUpdate,
        };

        const inputComponent = React.createElement(inputEditor as any, editorProps, null);

        return (
            <div className={"field " + expectedInput.type + "-expected-input"}>
                <label htmlFor={"expected-input_" + expectedInput.id}>
                    {expectedInput.name ? expectedInput.name : expectedInput.id}
                </label>
                {expectedInput.description ? <div>{expectedInput.description}</div> : null}
                {inputComponent}
            </div>
        );
    }
}
