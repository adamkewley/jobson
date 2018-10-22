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
import {Constants} from "../../Constants";
import {InputEditorUpdate} from "./updates/InputEditorUpdate";
import {InputEditorProps} from "./InputEditor";
import {Component, FormEvent, ReactElement} from "react";

export interface StringArrayInputEditorState {
    values: string[];
    coercionWarning: null | string;
}

export class StringArrayInputEditor extends Component<InputEditorProps, StringArrayInputEditorState> {

    private static coercePropValue({suggestedValue, expectedInput}: InputEditorProps): StringArrayInputEditorState {
        if (suggestedValue === undefined) {
            return {
                values: expectedInput.default || [],
                coercionWarning: null
            };
        } else if (!(suggestedValue instanceof Array)) {
            return {
                values: [],
                coercionWarning: `This input has been reset because the existing value was not an array of string (was ${typeof suggestedValue}.)`
            }
        } else {
            return {values: suggestedValue, coercionWarning: null};
        }
    }

    private static renderValueLabel(value: string, i: number): ReactElement<any> {
        return (
            <div className="ui horizontal label"
                 key={i}>
                {value}
            </div>
        );
    }

    private static renderInputSummary(values: string[]): ReactElement<any> {
        return (
            <div className="ui message">
                <div className="header">
                    {values.length} values
                </div>
                <ul>
                    <li>
                        First 5: {values.slice(0, 5).map(StringArrayInputEditor.renderValueLabel)}
                    </li>
                    <li>
                        Last 5: {values.slice(values.length - 5).map(StringArrayInputEditor.renderValueLabel)}
                    </li>
                </ul>
            </div>
        );
    }

    private static splitTextBlockIntoValues(text: string): string[] {
        return text.length === 0 ? [] : text.split(/[\n,]/);
    }


    public constructor(props: InputEditorProps) {
        super(props);

        const {values, coercionWarning} = StringArrayInputEditor.coercePropValue(props);

        this.state = {
            values: values,
            coercionWarning: coercionWarning,
        };
    }

    public componentDidMount(): void {
        this.onInputStateChange();
    }

    private onInputStateChange(): void {
        const update = InputEditorUpdate.value(this.state.values);
        this.props.onJobInputUpdate(update);
    }

    public render(): ReactElement<any> {
        return (
            <div>
                {this.state.coercionWarning !== null ? this.renderCocercionWarning() : null}
                {this.renderInput(this.state.values)}
                {this.renderHelperButtons(this.state.values)}
            </div>
        );
    }

    private renderCocercionWarning(): ReactElement<any> {
        return Helpers.renderWarningMessage("This input was changed", this.state.coercionWarning);
    }

    private renderInput(values: string[]): ReactElement<any> {
        return values.length < Constants.STR_ARRAY_INTERACTIVE_BREAKPOINT ?
            this.renderInteractiveInput(values) :
            StringArrayInputEditor.renderInputSummary(values);
    }

    private renderInteractiveInput(values: string[]): ReactElement<any> {
        return (
            <textarea value={values.join("\n")}
                      onChange={this.onInteractiveInputChanged.bind(this)}
                      placeholder="Entries separated by newlines"/>
        );
    }

    private onInteractiveInputChanged(e: FormEvent<HTMLTextAreaElement>): void {
        const values = StringArrayInputEditor.splitTextBlockIntoValues(e.currentTarget.value);

        if (!Helpers.deepEquals(values, this.state.values)) {
            this.setState({values}, () => this.onInputStateChange());
        }
    }

    private renderHelperButtons(values: string[]): ReactElement<any> {
        return (
            <div className="ui buttons">
                <button className="ui basic icon button"
                        onClick={this.onClickImportValuesFromFile.bind(this)}>
                    <i className="upload icon"/>
                    Import
                </button>

                <button className="ui basic icon button"
                        onClick={this.onClickDownloadValues.bind(this)}
                        disabled={values.length === 0}>
                    <i className="download icon"/>
                    Download
                </button>

                <button className="ui basic icon button"
                        onClick={this.onClickClearValues.bind(this)}
                        disabled={values.length === 0}>
                    <i className="remove icon"/>
                    Clear
                </button>
            </div>
        );
    }

    private onClickImportValuesFromFile(): void {
        Helpers
            .promptUserForFile("text/plain")
            .then(Helpers.readFileAsText)
            .then(text => text.trim())
            .then(StringArrayInputEditor.splitTextBlockIntoValues)
            .then(values => this.addValues(values))
            .catch(err => {
                alert(err);
                // User cancelled out of dialog: do nothing.
            });
    }

    private addValues(values: string[]): void {
        const newValues = this.state.values.concat(values);
        this.setState({values: newValues}, () => this.onInputStateChange());
    }

    private onClickDownloadValues(): void {
        const blobOfValuesJoinedByNewlines =
            new Blob([this.state.values.map(s => s + "\n").join("")], {type: "text/plain"});

        Helpers.promptUserToDownload(blobOfValuesJoinedByNewlines, "values.txt");
    }

    private onClickClearValues(): void {
        if (this.state.values.length > 0) {
            const newState = Object.assign({}, this.state, {values: []});
            this.setState(newState, () => this.onInputStateChange());
        }
    }
}
