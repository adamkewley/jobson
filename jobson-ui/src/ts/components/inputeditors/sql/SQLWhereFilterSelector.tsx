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
import {EqualsFilter} from "./filters/EqualsWhereFilter";
import {GreaterThanWhereFilter} from "./filters/GreaterThanWhereFilter";
import {LessThanWhereFilter} from "./filters/LessThanWhereFilter";
import {BetweenFilter} from "./filters/BetweenWhereFilter";
import {InWhereFilter} from "./filters/InWhereFilter";
import {NullWhereFilter} from "./filters/NullWhereFilter";
import {Component, FormEvent, ReactElement} from "react";

export interface SQLWhereFilterProps {
    column: any;
    onFilterChanged: (newFilter: string) => void;
}

export interface SQLWhereFilterSelectorProps {
    column: any;
    onNewFilter: (filter: string) => void;
}

export interface SQLWhereFilterSelectorState {
    filters: SQLWhereFilterDescriptor[];
    selectedFilterIdx: number,
}

export interface SQLWhereFilterDescriptor {
    name: string;
    ctor: (props: SQLWhereFilterProps) => Component<any>;
}

export class SQLWhereFilterSelector extends Component<SQLWhereFilterSelectorProps, SQLWhereFilterSelectorState> {

    private static filters: { [k: string]: SQLWhereFilterDescriptor } = {
        equals: {name: "Equal To", ctor: props => new EqualsFilter(props)},
        greaterThan: {name: "Greater Than", ctor: props => new GreaterThanWhereFilter(props)},
        lessThan: {name: "Less Than", ctor: props => new LessThanWhereFilter(props)},
        between: {name: "Between", ctor: props => new BetweenFilter(props)},
        inFilter: {name: "In", ctor: props => new InWhereFilter(props)},
        unfiltered: {name: "Unfiltered", ctor: props => new NullWhereFilter(props)},
    };

    private static getFiltersBySQLTypeString(typeStr: string): SQLWhereFilterDescriptor[] {
        const filtersThatApplyForSQLDatatype: { [k: string]: SQLWhereFilterDescriptor[] } = {
            byte: [
                SQLWhereFilterSelector.filters.unfiltered,
                SQLWhereFilterSelector.filters.equals,
                SQLWhereFilterSelector.filters.lessThan,
                SQLWhereFilterSelector.filters.between,
                SQLWhereFilterSelector.filters.inFilter,
            ],
            integer: [
                SQLWhereFilterSelector.filters.unfiltered,
                SQLWhereFilterSelector.filters.equals,
                SQLWhereFilterSelector.filters.greaterThan,
                SQLWhereFilterSelector.filters.lessThan,
                SQLWhereFilterSelector.filters.between,
                SQLWhereFilterSelector.filters.inFilter,
            ],
            int: [
                SQLWhereFilterSelector.filters.unfiltered,
                SQLWhereFilterSelector.filters.equals,
                SQLWhereFilterSelector.filters.greaterThan,
                SQLWhereFilterSelector.filters.lessThan,
                SQLWhereFilterSelector.filters.between,
                SQLWhereFilterSelector.filters.inFilter,
            ],
            short: [
                SQLWhereFilterSelector.filters.unfiltered,
                SQLWhereFilterSelector.filters.equals,
                SQLWhereFilterSelector.filters.greaterThan,
                SQLWhereFilterSelector.filters.lessThan,
                SQLWhereFilterSelector.filters.between,
                SQLWhereFilterSelector.filters.inFilter,
            ],
            long: [
                SQLWhereFilterSelector.filters.unfiltered,
                SQLWhereFilterSelector.filters.equals,
                SQLWhereFilterSelector.filters.greaterThan,
                SQLWhereFilterSelector.filters.lessThan,
                SQLWhereFilterSelector.filters.between,
                SQLWhereFilterSelector.filters.inFilter,
            ],
            float: [
                SQLWhereFilterSelector.filters.unfiltered,
                SQLWhereFilterSelector.filters.equals,
                SQLWhereFilterSelector.filters.greaterThan,
                SQLWhereFilterSelector.filters.lessThan,
                SQLWhereFilterSelector.filters.between,
                SQLWhereFilterSelector.filters.inFilter,
            ],
            double: [
                SQLWhereFilterSelector.filters.unfiltered,
                SQLWhereFilterSelector.filters.equals,
                SQLWhereFilterSelector.filters.greaterThan,
                SQLWhereFilterSelector.filters.lessThan,
                SQLWhereFilterSelector.filters.between,
                SQLWhereFilterSelector.filters.inFilter,
            ],
            char: [
                SQLWhereFilterSelector.filters.unfiltered,
                SQLWhereFilterSelector.filters.inFilter,
                SQLWhereFilterSelector.filters.equals,
            ],
            in: [
                SQLWhereFilterSelector.filters.unfiltered,
                SQLWhereFilterSelector.filters.inFilter,
                SQLWhereFilterSelector.filters.equals,
            ],
            enum: [
                SQLWhereFilterSelector.filters.unfiltered,
                SQLWhereFilterSelector.filters.inFilter,
                SQLWhereFilterSelector.filters.equals,
            ]
        };

        const typeStrWithoutOptionalAnnotations = typeStr.replace("?", "");

        if (typeStrWithoutOptionalAnnotations.includes("[")) {
            return [];
        } else if (typeStrWithoutOptionalAnnotations.includes("enum")) {
            return filtersThatApplyForSQLDatatype.enum;
        } else {
            return filtersThatApplyForSQLDatatype[typeStrWithoutOptionalAnnotations] || [];
        }
    }

    private static renderFilterOption(filter: any, i: number): ReactElement<any> {
        return <option key={i} value={i}>{filter.name}</option>;
    }

    private static renderCannotFilter(): ReactElement<any> {
        return (
            <div>
                <select className="filter-selector" disabled={true}>
                    <option>Cannot Filter Datatype</option>
                </select>
            </div>
        );
    }


    public constructor(props: SQLWhereFilterSelectorProps) {
        super(props);

        this.state = {
            filters: SQLWhereFilterSelector.getFiltersBySQLTypeString(props.column.type),
            selectedFilterIdx: 0,
        };
    }

    private onFilterChanged(e: FormEvent<HTMLSelectElement>): void {
        this.setState({
            selectedFilterIdx: parseInt(e.currentTarget.value),
        });
    }

    private onFilterValueChanged(newFilterVal: string): void {
        this.props.onNewFilter(newFilterVal);
    }


    public render(): ReactElement<any> {
        if (this.state.filters.length > 0) {
            return this.renderFilterSelector();
        } else {
            return SQLWhereFilterSelector.renderCannotFilter();
        }
    }

    private renderFilterSelector(): ReactElement<any> {
        const selectedFilterComponentCtor = this.state.filters[this.state.selectedFilterIdx].ctor;

        const selectedFilterComponentProps: SQLWhereFilterProps = {
            column: this.props.column,
            onFilterChanged: this.onFilterValueChanged.bind(this),
        };

        const selectedFilterComponent =
            React.createElement(selectedFilterComponentCtor as any, selectedFilterComponentProps, null);

        return (
            <div>
                <select className="filter-selector"
                        onChange={this.onFilterChanged.bind(this)}>
                    {this.state.filters.map(SQLWhereFilterSelector.renderFilterOption.bind(this))}
                </select>
                {selectedFilterComponent}
            </div>
        );
    }
}
