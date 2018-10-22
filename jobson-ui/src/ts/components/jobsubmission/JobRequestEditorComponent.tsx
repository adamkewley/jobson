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
import {InputEditor} from "../inputeditors/InputEditor";
import {InputEditorUpdate} from "../inputeditors/updates/InputEditorUpdate";
import {Constants} from "../../Constants";
import {Helpers} from "../../Helpers";
import {JobRequestEditorUpdate} from "./editorupdates/JobRequestEditorUpdate";
import {Component, FormEvent, ReactElement} from "react";
import {APIExpectedInput} from "../apitypes/APIExpectedInput";
import {JobsonAPI} from "../../JobsonAPI";
import {APIJobSpec} from "../apitypes/APIJobSpec";
import {APIJobRequest} from "../apitypes/APIJobRequest";

export interface JobRequestEditorComponentProps {
	api: JobsonAPI;
	routeProps: any;
	spec: APIJobSpec;
	suggestedJobRequest: any;
	onReqOrErrors: (update: JobRequestEditorUpdate) => void,
}

export interface JobRequestEditorComponentState {
	name: string,
	inputs: {[expectedInputId: string]: InputEditorUpdate}
}

/**
 * Component for editing job requests.
 *
 * Takes as input:
 * - A job spec
 * - A *suggested* job request that it is free to coerce
 *
 * Produces as output **EITHER**:
 * - A valid job request that can be submitted as-is to the API
 * - OR
 * - Errors (coercion, logic, etc.)
 */
export class JobRequestEditorComponent extends Component<JobRequestEditorComponentProps, JobRequestEditorComponentState> {

	private static createMissingInputsForAllExpectedInputs(expectedInputs: APIExpectedInput[]): {[expectedInputId: string]: InputEditorUpdate} {
		const ret: {[expectedInputId: string]: InputEditorUpdate} = {};
		expectedInputs.forEach(expectedInput => ret[expectedInput.id] = InputEditorUpdate.missing());
		return ret;
	}


	public constructor(props: JobRequestEditorComponentProps, context: any) {
		super(props, context);

		this.state = {
			name: props.suggestedJobRequest.name || Constants.DEFAULT_JOB_NAME,
			inputs: JobRequestEditorComponent.createMissingInputsForAllExpectedInputs(props.spec.expectedInputs),
		};
	}


	public componentDidMount(): void {
		this.onJobRequestStateChange();
	}

	public render(): ReactElement<any> {
		return (
			<div className="ui form">
				<h2>Edit Job</h2>
				{this.renderSystemWideExpectedInputs()}
				{this.renderJobSpecExpectedInputs()}
			</div>
		);
	}

	private renderSystemWideExpectedInputs(): ReactElement<any> {
		return (
			<div className="field">
				<label htmlFor="job-name">
					Job Name
				</label>
				<div className="ui fluid input">
					<input type="text"
								 id="job-name"
								 placeholder="Job Name"
								 value={this.state.name}
								 className="ui fluid input"
								 onChange={this.onJobNameChanged.bind(this)} />
				</div>
			</div>
		);
	}

	private onJobNameChanged(e: FormEvent<HTMLInputElement>): void {
		const newName = e.currentTarget.value;

		if (newName !== this.state.name) {
			this.setState({name: newName}, () => this.onJobRequestStateChange());
		}
	}

	private renderJobSpecExpectedInputs(): ReactElement<any>[] {
		return this.props.spec
			.expectedInputs
			.map(this.renderJobSpecExpectedInput.bind(this));
	}

	private renderJobSpecExpectedInput(expectedInput: APIExpectedInput): ReactElement<any> {
		const suggestedValue = this.props.suggestedJobRequest.inputs[expectedInput.id];

		return (
			<InputEditor
				suggestedValue={suggestedValue}
				expectedInput={expectedInput}
				onJobInputUpdate={this.onJobInputUpdate.bind(this, expectedInput.id)}
				key={"SPEC_" + this.props.spec.id + "_INPUT_" + expectedInput.id} />
		);
	}

	private onJobInputUpdate(expectedInputId: string, inputUpdate: InputEditorUpdate): void {
		const oldJobInputs = this.state.inputs;
		const oldJobInput = oldJobInputs[expectedInputId];

		if (!Helpers.deepEquals(oldJobInput, inputUpdate)) {
			this.setStateWithNewInput(expectedInputId, inputUpdate);
		}
	}

	private setStateWithNewInput(expectedInputId: string, inputUpdate: InputEditorUpdate): void {
		// Done like this because updates may happen concurrently.
		this.setState(oldState => {
			const newJobInputs = Object.assign({}, oldState.inputs, {[expectedInputId]: inputUpdate});
            return Object.assign({}, oldState, {inputs: newJobInputs});
		}, () => this.onJobRequestStateChange());
	}

	private onJobRequestStateChange(): void {
		const completeInputs: {[inputId: string]: any} = {};
		const missingInputsWithNoDefault: string[] = [];
		const erroneousInputs: {[inputId: string]: string} = {};

		Helpers.forEachKv((inputId, maybeInput) => {
			maybeInput.accept({
				visitValue: value => {
					completeInputs[inputId] = value;
				},
				visitMissing: () => {
					const expectedInput = this.props.spec.expectedInputs.find(ee => ee.id === inputId);
					if (!expectedInput.default) {
						missingInputsWithNoDefault.push(inputId);
					}
				},
				visitErrors: errors => {
					erroneousInputs[inputId] = errors.join(",");
				},
			});
		}, this.state.inputs);

		const update =
			(Helpers.isEmpty(erroneousInputs) && missingInputsWithNoDefault.length === 0) ?
				JobRequestEditorUpdate.value(this.createJobReq(completeInputs)) :
				JobRequestEditorUpdate.errors(this.combineErrors(missingInputsWithNoDefault, erroneousInputs));

		this.props.onReqOrErrors(update);
	}

	private createJobReq(completeInputs: {[inputId: string]: any}): APIJobRequest {
		return {
			spec: this.props.spec.id,
			name: this.state.name,
			inputs: completeInputs,
		};
	}

	private combineErrors(missingInputsWithNoDefault: string[], erroneousInputs: {[inputId: string]: string}): string[] {
		// TODO: cleanup
		const errMsgs: string[] = [];

		missingInputsWithNoDefault.forEach(inputName => {
			errMsgs.push(`${inputName}: is missing`);
		});

		Helpers.forEachKv((inputId, err) => {
			errMsgs.push(`${inputId}: ${err}`);
		}, erroneousInputs);

		return errMsgs;
	}
}
