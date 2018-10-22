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
import {SQLQueryBuilder} from "./sql/SQLQueryBuilder";
import {SelectInputEditor} from "./SelectInputEditor";
import {Helpers} from "../../Helpers";
import {InputEditorUpdate} from "./updates/InputEditorUpdate";
import {InputEditorProps} from "./InputEditor";
import {Component, ReactElement} from "react";
import {APIExpectedInput} from "../apitypes/APIExpectedInput";

export interface SQLInputEditorState {
    query: string;
    coercionWarning: null | string;
    selectedTable: any;
}

export class SQLInputEditor extends Component<InputEditorProps, SQLInputEditorState> {

    private static coercePropValue(suggestedValue: any): { query: string, coercionWarning: null | string } {
        if (suggestedValue === undefined) {
            return {
                query: "",
                coercionWarning: null
            };
        } else {
            return {
                query: suggestedValue,
                coercionWarning: `This input has been reset because Jobson UI cannot copy SQL queries.`
            };
        }
    }


    public constructor(props: InputEditorProps) {
        super(props);

        const {query, coercionWarning} = SQLInputEditor.coercePropValue(props.suggestedValue);

        this.state = {
            query: query,
            selectedTable: props.expectedInput.tables[0],
            coercionWarning: coercionWarning,
        };
    }

    public componentDidMount(): void {
        this.onInputStateChange();
    }

    private onInputStateChange(): void {
        const update = InputEditorUpdate.value(this.state.query);
        this.props.onJobInputUpdate(update);
    }

    public render(): ReactElement<any> {
        return (
            <div>
                {this.state.coercionWarning !== null ? this.renderCoercionWarning() : null}
                {this.renderTableSelectField()}
                {this.renderBuildQueryField()}
            </div>
        );
    }

    private renderCoercionWarning(): ReactElement<any> {
        return Helpers.renderWarningMessage("This input was changed", this.state.coercionWarning);
    }

    private renderTableSelectField(): ReactElement<any> {
        return (
            <div className="field">
                <label>Table</label>
                <SelectInputEditor
                    expectedInput={SQLInputEditor.createFakeExpectedInput(this.props.expectedInput.tables)}
                    onJobInputUpdate={this.onTableSelectionChange.bind(this)}
                    suggestedValue={undefined}
                    key={"key-not-used-here"} />
            </div>
        );
    }

    private static createFakeExpectedInput(options: any[]): APIExpectedInput {
        // TODO: refactor this out
        return {
            id: "ignore",
            type: "select",
            name: "some select",
            description: "not used",
            default: undefined,
            options: options,
        };
    }

    private onTableSelectionChange(selectInputUpdate: InputEditorUpdate): void {
        selectInputUpdate.accept({
            visitValue: value => {
                const selectedTableId = value;
                const selectedTable =
                    this.props.expectedInput.tables.find((t: any) => t.id === selectedTableId);

                if (selectedTable !== this.state.selectedTable) {
                    const newState = Object.assign({}, this.state, {selectedTable});
                    this.setState(newState, () => this.onInputStateChange());
                }
            },
            visitMissing: () => {
            },
            visitErrors: () => {
            }, // TODO: Handle this
        });
    }

    private renderBuildQueryField(): ReactElement<any> {
        return (
            <div className="field">
                <label>Query</label>
                <SQLQueryBuilder
                    table={this.state.selectedTable}
                    onQueryChanged={this.onQueryChanged.bind(this)}/>
            </div>
        );
    }

    private onQueryChanged(query: string): void {
        if (query !== this.state.query) {
            const newState = Object.assign({}, this.state, {query});
            this.setState(newState, () => this.onInputStateChange());
        }
    }
}
