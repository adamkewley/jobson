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
import {SQLWhereFilterProps} from "../SQLWhereFilterSelector";
import {Component, FormEvent, ReactElement} from "react";

export interface BetweenWhereFilterState {
    min: string;
    max: string;
}

export class BetweenFilter extends Component<SQLWhereFilterProps, BetweenWhereFilterState> {

    public constructor(props: SQLWhereFilterProps) {
        super(props);

        this.state = {
            min: "",
            max: "",
        };
    }


    public render(): ReactElement<any> {
        return (
            <div>
                <input type="text" placeholder="min" onChange={this.onMinChanged.bind(this)}/>
                <input type="text" placeholder="max" onChange={this.onMaxChanged.bind(this)}/>
            </div>
        );
    }

    private onMinChanged(e: FormEvent<HTMLInputElement>): void {
        this.setState({
            min: e.currentTarget.value,
        }, this.onFilterChanged.bind(this));
    }

    private onMaxChanged(e: FormEvent<HTMLInputElement>): void {
        this.setState({
            max: e.currentTarget.value
        }, this.onFilterChanged.bind(this));
    }

    private onFilterChanged(): void {
        const col = this.props.column.id;
        const newFilter = `${this.state.min} < ${col} and ${col} < ${this.state.max}`;

        this.props.onFilterChanged(newFilter);
    }
}
