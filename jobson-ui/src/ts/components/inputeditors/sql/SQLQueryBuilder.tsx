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
import {SQLWhereFilterSelector} from "./SQLWhereFilterSelector";
import {Helpers} from "../../../Helpers";
import {Component, FormEvent, ReactElement} from "react";

export interface SQLQueryBuilderProps {
    table: any;
    onQueryChanged: (query: string) => void;
}

export interface SQLQueryBuilderState {
    table: any;
    columnsToExtract: Set<string>;
    filters: { [columnId: string]: string };
    search: string;
    onlyShowSelectedColumns: boolean;
}

export class SQLQueryBuilder extends Component<SQLQueryBuilderProps, SQLQueryBuilderState> {

    public constructor(props: SQLQueryBuilderProps) {
        super(props);

        this.state = {
            table: props.table,
            columnsToExtract: new Set(),
            filters: {},
            search: "",
            onlyShowSelectedColumns: false,
        };
    }

    public componentDidMount(): void {
        this.props.onQueryChanged(this.toSqlQuery());
    }

    public componentWillReceiveProps(newProps: SQLQueryBuilderProps): void {
        if (newProps.table !== this.state.table) {
            this.setState({
                table: newProps.table,
                columnsToExtract: new Set(),
                filters: {},
            }, () => newProps.onQueryChanged(this.toSqlQuery()));
        }
    }

    private onExtractCheckboxChange(column: any, e: FormEvent<HTMLInputElement>): void {
        const columnsToExtract = new Set(this.state.columnsToExtract);

        if (e.currentTarget.checked) columnsToExtract.add(column.id);
        else columnsToExtract.delete(column.id);

        this.setState({columnsToExtract}, () => {
            this.props.onQueryChanged(this.toSqlQuery());
        });
    }

    private onNewFilter(column: any, filterStr: string | null): void {
        const filters = filterStr !== null ?
            Object.assign({}, this.state.filters, {[column.id]: filterStr}) :
            Helpers.dissasoc(this.state.filters, column.id);

        this.setState({filters}, () => {
            this.props.onQueryChanged(this.toSqlQuery());
        });
    }

    private onSearchChanged(e: FormEvent<HTMLInputElement>): void {
        this.setState({
            search: e.currentTarget.value
        });
    }

    private shouldDisplayColumn(column: any): boolean {
        const columnSelected = this.state.columnsToExtract.has(column.id);

        if (this.state.onlyShowSelectedColumns && !columnSelected) {
            return false;
        } else {
            return column.id.toLowerCase().includes(this.state.search.toLowerCase());
        }
    }

    private onClickedSelectAll(): void {
        const allTableColumnIds = this.state.table.columns.map((col: any) => col.id);

        this.setState({
            columnsToExtract: new Set(allTableColumnIds),
        });
    }

    private onClickedClearSelection(): void {
        this.setState({
            columnsToExtract: new Set(),
        });
    }

    private toSqlQuery(): string {
        const notEmptyFilters =
            Object.keys(this.state.filters).map(k => this.state.filters[k]);

        const whereClause = notEmptyFilters.length > 0 ?
            "\nwhere " + notEmptyFilters.join(" and\n") :
            "";

        return `select ${Array.from(this.state.columnsToExtract).join(", ")}
from ${this.state.table.id}${whereClause};`;
    }

    private renderColumn(column: any): ReactElement<any> {
        return (
            <tr key={column.id}>
                <td>
                    <input type="checkbox"
                           checked={this.state.columnsToExtract.has(column.id)}
                           onChange={this.onExtractCheckboxChange.bind(this, column)}/>
                </td>
                <td>{column.name} - {column.description}</td>
                <td>
                    <SQLWhereFilterSelector column={column} onNewFilter={this.onNewFilter.bind(this, column)}/>
                </td>
            </tr>
        );
    }

    public render(): ReactElement<any> {
        return (
            <div>
                {this.renderTableFilters()}

                <div className="table-wrapper">
                    {this.renderTable()}
                </div>
            </div>
        );
    }

    private renderTableFilters(): ReactElement<any> {
        return (
            <div className="inline fields">

                <div className="ui left icon input"
                     style={{marginRight: "1em", width: "auto"}}>
                    <i className="search icon"/>
                    <input type="search"
                           placeholder="Search"
                           value={this.state.search}
                           onChange={this.onSearchChanged.bind(this)}/>
                </div>
                <button className="ui tiny button" onClick={this.onClickedSelectAll.bind(this)}>
                    Select All
                </button>

                <button className="ui tiny button"
                        onClick={this.onClickedClearSelection.bind(this)}
                        disabled={this.state.columnsToExtract.size === 0}>
                    Clear Selection
                </button>

                <div className="ui checkbox">
                    <input type="checkbox"
                           id="only-show-selected-columns"
                           checked={this.state.onlyShowSelectedColumns}
                           onChange={(e) => this.setState({onlyShowSelectedColumns: e.target.checked})}
                           disabled={this.state.onlyShowSelectedColumns === false && this.state.columnsToExtract.size === 0}/>
                    <label htmlFor="only-show-selected-columns">
                        Only show selected columns
                    </label>
                </div>
            </div>
        );
    }

    private renderTable(): ReactElement<any> {
        return (
            <table className="ui compact celled table">
                <colgroup>
                    <col width="10%"/>
                    <col width="40%"/>
                    <col width="50%"/>
                </colgroup>
                <thead>
                <tr>
                    <th>Extract?</th>
                    <th>Description</th>
                    <th>Filter</th>
                </tr>
                </thead>
                {this.renderTableBody()}
            </table>
        );
    }

    private renderTableBody(): ReactElement<any> {
        const rows =
            this.state.table.columns
                .filter(this.shouldDisplayColumn.bind(this))
                .map(this.renderColumn.bind(this));

        return rows.length === 0 ?
            (
                <tbody>
                <tr>
                    <td className="missing-banner" colSpan={4}>
                        No rows to show - are you filtering too much?
                    </td>
                </tr>
                </tbody>
            ) :
            (
                <tbody>
                {rows}
                </tbody>
            );
    }
}
