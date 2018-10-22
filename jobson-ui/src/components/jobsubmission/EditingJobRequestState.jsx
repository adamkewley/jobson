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
import {JobRequestEditorComponent} from "./JobRequestEditorComponent";
import {JobSpecSelectorComponent} from "./JobSpecSelectorComponent";
import {Helpers} from "../../Helpers";
import {LoadingJobSpecState} from "./LoadingJobSpecState";
import {JobRequestEditorUpdate} from "./editorupdates/JobRequestEditorUpdate";

/**
 * State that occurs when the a job spec and (potentially) job request
 * pair is loaded and the user is expected to edit the request pre-submission.
 *
 * The job request may have been loaded from an existing request. It is this
 * component's responsibility to:
 *
 * - Handle job spec changes (e.g. client selects different spec)
 * - Show job submission validation errors
 * - Show job editing errors
 * - Perform job submission, when the editor produces a valid job request
 */
export class EditingJobRequestState extends React.Component {

	static renderReqError(error, key) {
		return <li key={key}>{error}</li>;
	}


	constructor(props) {
		super(props);

		this.state = {
			reqOrErrors: JobRequestEditorUpdate.errors([]),
			isSubmittingJob: false,
			jobSubmissionError: null,
		};
	}


	render() {
		return (
			<div>
				<JobSpecSelectorComponent
					selectedSpecId={this.props.jobSpec.id}
					specs={this.props.jobSpecs}
					onSelectedSpecIdChanged={this.onSelectedSpecIdChanged.bind(this)} />

				<br />

				{this.state.jobSubmissionError ? this.renderJobSubmissionError() : null}

				<JobRequestEditorComponent
					api={this.props.api}
					routeProps={this.props.routeProps}
					spec={this.props.jobSpec}
					suggestedJobRequest={this.props.partialJobRequest}
					onReqOrErrors={this.onReqOrErrors.bind(this)} />

				{this.state.reqOrErrors.accept({ visitValue: () => this.renderJobSubmissionButtons(), visitErrors: () =>this.renderReqErrors()})}
			</div>
		);
	}

	onSelectedSpecIdChanged(newId) {
		this.transitionToLoadingJobSpec(newId);
	}

	transitionToLoadingJobSpec(newId) {
		const req = this.state.reqOrErrors.accept({
			visitValue: value => value,
			visitErrors: () => this.props.partialJobRequest,
		});

		const props = {
			api: this.props.api,
			routeProps: this.props.routeProps,
			onTransitionRequest: this.props.onTransitionRequest,
			jobSpecs: this.props.jobSpecs,
			partialJobRequest: req,
			specId: newId,
		};
		const loadJobSpecComponent = React.createElement(LoadingJobSpecState, props, null);

		this.props.onTransitionRequest(loadJobSpecComponent);
	}

	onReqOrErrors(reqOrErrors) {
		this.setState({reqOrErrors});
	}

	renderJobSubmissionError() {
		return Helpers.renderAPIErrorMessage("submitting job", this.state.jobSubmissionError);
	}

	renderJobSubmissionButtons() {
		return (
			<div style={{marginTop: "1em", textAlign: "center"}}>
				<div>
					<button className="ui primary button"
									onClick={this.onUserClickedSubmit.bind(this)}>
						Submit Job
					</button>
				</div>

				{this.renderDownloadRequestButton()}
			</div>
		);
	}

	renderDownloadRequestButton() {
		return (
			<button className="ui tiny button"
							onClick={this.onUserClickedDownloadRequest.bind(this)}>
				Download Request (debug)
			</button>
		);
	}

	onUserClickedSubmit() {
		this.state.reqOrErrors.accept({
			visitValue: jobRequest => this.trySubmitJobRequestToAPI(jobRequest),
			visitErrors: this.onJobSubmissionFailure({ message: "Cannot submit job to API: the job contains errors" }),
		});
	}

	trySubmitJobRequestToAPI(jobRequest) {
		this.setState({
			isSubmittingJob: true,
			jobSubmissionError: null,
		}, () => {
			this.props.api.submitJobRequest(jobRequest)
				.then(resp => this.onJobSubmissionSuccess(resp))
				.catch(err => this.onJobSubmissionFailure(err));
		});
	}

	onJobSubmissionSuccess(apiResponse)  {
		this.navigateToNewlyCreatedJob(apiResponse);
	}

	navigateToNewlyCreatedJob(apiResp) {
		this.props.routeProps.history.push(`/jobs/${apiResp.id}`);
	}

	onJobSubmissionFailure(apiError) {
		this.setState({
			isSubmittingJob: false,
			jobSubmissionError: apiError,
		}, () => window.scrollTo(0, 0));
	}

	onUserClickedDownloadRequest() {
		this.state.reqOrErrors.accept({
			visitValue: jobRequest => Helpers.promptUserToDownloadAsJSON(jobRequest),
			visitErrors: () => {
				console.error("Incorrect state detected: attempted to download a request when errors present: skipping");
			},
		});
	}

	renderReqErrors() {
		return this.state.reqOrErrors.accept({
			visitValue: () => {
				return Helpers.renderErrorMessage("UI error (bug)", "Incorrect state detected: attempted to render error messages when no errors are present in job");
			},
			visitErrors: errors => {
				return (
					<div className="ui negative icon message">
						<i className="warning circle icon"></i>
						<div className="content">
							<div className="header">
								Cannot Submit: Errors in input
							</div>

							<ul>
								{errors.map(EditingJobRequestState.renderReqError.bind(this))}
							</ul>
						</div>
					</div>
				);
			}
		});
	}
}
