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

import React from "react";
import {EqualsFilter} from "./filters/EqualsWhereFilter";
import {GreaterThanWhereFilter} from "./filters/GreaterThanWhereFilter";
import {LessThanWhereFilter} from "./filters/LessThanWhereFilter";
import {BetweenFilter} from "./filters/BetweenWhereFilter";
import {InWhereFilter} from "./filters/InWhereFilter";
import {NullWhereFilter} from "./filters/NullWhereFilter";

export class SQLWhereFilterSelector extends React.Component {

	static filters = {
		equals: { name: "Equal To", ctor: EqualsFilter },
		greaterThan: { name: "Greater Than", ctor: GreaterThanWhereFilter },
		lessThan: { name: "Less Than", ctor: LessThanWhereFilter },
		between: { name: "Between", ctor: BetweenFilter },
		inFilter: { name: "In", ctor: InWhereFilter },
		unfiltered: { name: "Unfiltered", ctor: NullWhereFilter },
	};

	static getFiltersBySQLTypeString(typeStr) {
		const filtersThatApplyForSQLDatatype = {
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

	static renderFilterOption(filter, i) {
		return <option key={i} value={i}>{filter.name}</option>;
	}

	static renderCannotFilter() {
		return (
			<div>
				<select className="filter-selector"
								disabled={true}>
					<option>Cannot Filter Datatype</option>
				</select>
			</div>
		);
	}


	constructor(props) {
		super(props);

		this.state = {
			filters: SQLWhereFilterSelector.getFiltersBySQLTypeString(props.column.type),
			selectedFilterIdx: 0,
		};
	}

	onFilterChanged(e) {
		this.setState({
			selectedFilterIdx: e.target.value,
		});
	}

	onFilterValueChanged(newFilterVal) {
		this.props.onNewFilter(newFilterVal);
	}


	render() {
		if (this.state.filters.length > 0)
			return this.renderFilterSelector();
		else
			return SQLWhereFilterSelector.renderCannotFilter();
  }

	renderFilterSelector() {
		const selectedFilterComponentCtor = this.state.filters[this.state.selectedFilterIdx].ctor;

		const selectedFilterComponentProps = {
			column: this.props.column,
			onFilterChanged: this.onFilterValueChanged.bind(this),
		};

		const selectedFilterComponent =
			React.createElement(selectedFilterComponentCtor, selectedFilterComponentProps, null);

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
