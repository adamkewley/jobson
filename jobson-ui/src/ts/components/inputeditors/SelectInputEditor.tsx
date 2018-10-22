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

export interface SelectInputEditorState {
    selectedOption: string;
    coercionWarning: null | string;
}

export class SelectInputEditor extends Component<InputEditorProps, SelectInputEditorState> {

    private static coerceValueFromProps(props: InputEditorProps): SelectInputEditorState {
        if (props.suggestedValue === undefined) {
            return {
                selectedOption: props.suggestedValue || props.expectedInput.options[0].id,
                coercionWarning: null
            };
        } else if (!SelectInputEditor.optionsContainsValue(props.expectedInput.options, props.suggestedValue)) {
            return {
                selectedOption: props.expectedInput.options[0].id,
                coercionWarning: `This field was reset because the existing value, '${props.suggestedValue}', is not one of the available options. This is probably because '${props.suggestedValue}' was removed from the job spec.`
            };
        } else {
            return {
                selectedOption: props.suggestedValue,
                coercionWarning: null
            };
        }
    }

    private static optionsContainsValue(options: { id: string }[], value: string): boolean {
        return options.map(o => o.id).indexOf(value) !== -1;
    }

    private static renderOption(option: any, i: number): ReactElement<any> {
        return (
            <option key={i} value={option.id}>
                {option.name || option.id}
            </option>
        );
    }


    public constructor(props: InputEditorProps) {
        super(props);

        this.state = SelectInputEditor.coerceValueFromProps(props);
    }


    public componentDidMount(): void {
        this.onInputStateChange();
    }

    private onInputStateChange(): void {
        const update = InputEditorUpdate.value(this.state.selectedOption);
        this.props.onJobInputUpdate(update);
    }

    public render(): ReactElement<any> {
        return (
            <div>
                {this.state.coercionWarning !== null ? this.renderCoercionWarning() : null}
                {this.renderOptionSelector()}
                {this.renderSelectionDescription()}
            </div>
        );
    }

    private renderCoercionWarning(): ReactElement<any> {
        return Helpers.renderWarningMessage("This input was changed", this.state.coercionWarning);
    }

    private renderOptionSelector(): ReactElement<any> {
        return (
            <select id={"expected-input_" + this.props.expectedInput.id}
                    onChange={this.onSelectionChangedInUI.bind(this)}
                    value={this.state.selectedOption}>
                {this.props.expectedInput.options.map(SelectInputEditor.renderOption)}
            </select>
        );
    }

    private onSelectionChangedInUI(e: FormEvent<HTMLSelectElement>): void {
        if (e.currentTarget.value !== this.state.selectedOption) {
            this.setState({selectedOption: e.currentTarget.value}, () => this.onInputStateChange());
        }
    }

    private renderSelectionDescription(): ReactElement<any> | null {
        const options = this.props.expectedInput.options;
        const selectedOption = options.find((o: any) => o.id === this.state.selectedOption);

        if (selectedOption.description) {
            return (
                <div className="ui info message">
                    {selectedOption.description}
                </div>
            );
        } else {
            return null;
        }
    }
}
