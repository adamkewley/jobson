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
import {Helpers} from "../../Helpers";
import {InputEditorUpdate} from "./updates/InputEditorUpdate";
import {InputEditorProps} from "./InputEditor";
import {Component, FormEvent, ReactElement} from "react";
import {APIExpectedInput} from "../apitypes/APIExpectedInput";
import {string} from "prop-types";
import {type} from "os";

export interface StringInputEditorState {
    value: string;
    coercionWarning: string | null;
}

export class StringInputEditor extends Component<InputEditorProps, StringInputEditorState> {

    private static coercePropValue(suggestedValue: any, expectedInput: APIExpectedInput): StringInputEditorState {
        if (suggestedValue === undefined) {
            return {
                value: expectedInput.default || "",
                coercionWarning: null
            };
        } else if (typeof suggestedValue !== 'string') {
            return {
                value: "",
                coercionWarning: `The supplied value was not a string (was ${typeof suggestedValue}). This field was reset`
            };
        } else {
            return {
                value: suggestedValue as string,
                coercionWarning: null
            };
        }
    }


    public constructor(props: InputEditorProps) {
        super(props);

        this.state = StringInputEditor.coercePropValue(props.suggestedValue, props.expectedInput);
    }


    public componentDidMount(): void {
        this.onInputStateChange();
    }

    private onInputStateChange(): void {
        const update = InputEditorUpdate.value(this.state.value);
        this.props.onJobInputUpdate(update);
    }

    public render(): ReactElement<any> {
        return (
            <div className="ui fluid input">
                {this.state.coercionWarning !== null ? this.renderCoercionWarning() : null}
                {this.renderInput()}
            </div>
        );
    }

    private renderCoercionWarning(): ReactElement<any> {
        return Helpers.renderWarningMessage("This input was changed", this.state.coercionWarning);
    }

    private renderInput(): ReactElement<any> {
        return (
            <input type="text"
                   id={"expected-input_" + this.props.expectedInput.id}
                   value={this.state.value}
                   onChange={this.onInputElementChange.bind(this)}/>
        );
    }

    private onInputElementChange(e: FormEvent<HTMLInputElement>): void {
        if (e.currentTarget.value !== this.state.value) {
            this.setState(
                {value: e.currentTarget.value},
                () => this.onInputStateChange());
        }
    }
}
