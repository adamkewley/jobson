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
import {StringArrayInputEditor} from "../../StringArrayInputEditor";
import {SQLWhereFilterProps} from "../SQLWhereFilterSelector";
import {Component, ReactElement} from "react";
import {InputEditorUpdate} from "../../updates/InputEditorUpdate";
import {APIExpectedInput} from "../../../apitypes/APIExpectedInput";

export interface InWhereFilterState {
    entries: string[];
}

export class InWhereFilter extends Component<SQLWhereFilterProps, InWhereFilterState> {

    private static createMockStringArrayExpectedInput(): APIExpectedInput {
        return {
            id: "mock",
            type: "string[]",
            name: "mock",
            description: "mock",
            default: undefined,
        };  // TODO: remove this unnecessary stuff.
    }


    public constructor(props: SQLWhereFilterProps) {
        super(props);

        this.state = {
            entries: [],
        };
    }


    public render(): ReactElement<any> {
        return (
            <StringArrayInputEditor
                suggestedValue={this.state.entries}
                expectedInput={InWhereFilter.createMockStringArrayExpectedInput()}
                onJobInputUpdate={this.onValueChanged.bind(this)}
                key={"mock"}/>  // TODO: remove from input editor props
        );
    }

    private onValueChanged(maybeVals: InputEditorUpdate) {
        maybeVals.accept({
            visitValue: vals => {
                this.setState({entries: vals}, () => {
                    const filterText = this.constructSqlInFilterFromStringArray(vals);
                    this.props.onFilterChanged(filterText);
                });
            },
            visitErrors: () => {
            },  // TODO: fix
            visitMissing: () => {
            },  // TODO: fix
        });
    }

    private constructSqlInFilterFromStringArray(vals: string[]): string {
        const isStringOrEnum = this.props.column.type === "string" || this.props.column.type.startsWith("enum");

        const escapedVals = isStringOrEnum ? vals.map(v => `'${v}'`) : vals;

        return `${this.props.column.id} IN (${escapedVals.join(", ")})`;
    }
}
