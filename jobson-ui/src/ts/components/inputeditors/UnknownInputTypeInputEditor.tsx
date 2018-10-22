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

import {Helpers} from "../../Helpers";
import {InputEditor, InputEditorProps} from "./InputEditor";
import {InputEditorUpdate} from "./updates/InputEditorUpdate";
import {Component} from "react";

export class UnknownInputTypeInputEditor extends Component<InputEditorProps> {

	componentDidMount() {
		const err = this.generateErrorHeader();
		const update = InputEditorUpdate.errors([err]);
		this.props.onJobInputUpdate(update);
	}

	generateErrorHeader() {
		const expectedInput = this.props.expectedInput;
		return `${expectedInput.id}: Has an unknown input type (${expectedInput.type})`;
	}

	render() {
		const expectedInput = this.props.expectedInput;

		const header = this.generateErrorHeader();
		const body = `${expectedInput.id} has a type of '${expectedInput.type}'. Although this datatype might be supported by
		              the Jobson backend, it is not supported by the UI. Types supported by the ui are: ${InputEditor.getSupportedInputEditors().join(", ")}`;

		return Helpers.renderErrorMessage(header, body);
	}
}
