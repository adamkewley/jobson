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

export interface IntegerInputEditorArgs {
    min: number;
    max: number;
    typeName: string;
}

export interface IntegerInputEditorState {
    rawValue: string;
    error: null | string;
}

export class IntegerInputEditor extends Component<InputEditorProps, IntegerInputEditorState> {

    private readonly min: number;
    private readonly max: number;
    private readonly typeName: string;


    public constructor({min, max, typeName}: IntegerInputEditorArgs, props: InputEditorProps) {
        super(props);

        this.min = min;
        this.max = max;
        this.typeName = typeName;

        const rawValue = props.suggestedValue || props.expectedInput.default || "";

        this.state = {
            rawValue: rawValue,
            error: this.validateRawValue(rawValue),
        };
    }


    public componentDidMount(): void {
        this.onInputStateChange();
    }

    public render(): ReactElement<any> {
        return (
            <div>
                {this.state.error !== null ? Helpers.renderErrorMessage("Error", this.state.error) : null}
                {this.renderInput()}
            </div>
        );
    }

    private renderInput(): ReactElement<any> {
        return <input type="text"
                      value={this.state.rawValue}
                      onChange={this.onInputChanged.bind(this)}/>;
    }

    private onInputChanged(e: FormEvent<HTMLInputElement>): void {
        this.onNewRawValue(e.currentTarget.value);
    }

    private onNewRawValue(newRawValue: string): void {
        if (newRawValue !== this.state.rawValue) {
            this.setState(
                {rawValue: newRawValue, error: this.validateRawValue(newRawValue)},
                () => this.onInputStateChange());
        }
    }

    private validateRawValue(rawVal: string): null | string {
        const val = parseInt(rawVal);

        if (rawVal.length === 0) {
            return null;
        } else if (isNaN(val)) {
            return `${rawVal}: is not a number`;
        } else if (val < this.min) {
            return `${rawVal}: too small: minimum value allowed for an ${this.typeName} input is ${this.min}`;
        } else if (val > this.max) {
            return `${rawVal}: too big: maximum value allowed for an ${this.typeName} input is ${this.max}`;
        } else {
            return null;
        }
    }

    private onInputStateChange(): void {
        this.props.onJobInputUpdate(this.createJobInputEditorUpdateFromState());
    }

    private createJobInputEditorUpdateFromState(): InputEditorUpdate {
        if (this.state.error !== null) {
            return InputEditorUpdate.errors([this.state.error]);
        } else if (this.state.rawValue.length === 0) {
            return InputEditorUpdate.missing();
        } else {
            const val = parseInt(this.state.rawValue);
            return val.toString() !== this.state.rawValue ?
                InputEditorUpdate.value(this.state.rawValue) :  // Javascript can't represent the number in binary, use string representation
                InputEditorUpdate.value(val);
        }
    }
}
