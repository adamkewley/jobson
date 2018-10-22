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
import {LoadingJobSpecState, LoadingJobSpecStateProps} from "./LoadingJobSpecState";
import {Helpers} from "../../Helpers";
import {LoadExistingJobRequestState} from "./LoadExistingJobRequestState";
import {JobsonAPI} from "../../JobsonAPI";
import {ReactElement} from "react";
import {APIErrorMessage} from "../apitypes/APIErrorMessage";
import {APIJobSpecSummary} from "../apitypes/APIJobSpecSummary";
import {APIJobRequest} from "../apitypes/APIJobRequest";

export interface LoadingJobSpecsStateProps {
	api: JobsonAPI,
	routeProps: any,
	onTransitionRequest: (el: ReactElement<any>) => void,
}

export interface LoadingJobSpecsStateState {
	isLoading: boolean,
	loadingError: null | APIErrorMessage,
	apiReturnedNoJobSpecs: boolean,
}

/**
 * Initial job submission state: load the job specs list and
 * transition to either basing a job request on an existing
 * request or creating a new request.
 */
export class LoadingJobSpecsState extends React.Component<LoadingJobSpecsStateProps, LoadingJobSpecsStateState> {

	static renderLoadingMessage() {
		return Helpers.renderLoadingMessage("job specs");
	}

	static createBlankPartialJobRequest(): APIJobRequest {
		return {
			spec: null,
			name: "default",
			inputs: {},
		};
	}

	static renderNoJobSpecsAvailableMessage() {
		return (
			<div className="ui info icon message">
				<i className="info circle icon"/>
				<div className="content">
					<div className="header">
						No jobs yet!
					</div>
					<p>
						The server doesn't appear to have any jobs you can submit.
						This is probably because the server hasn't been configured
						with any job specs yet. The server admin should add job specs
						to this system (e.g. with <code>jobson generate spec</code>)
					</p>
				</div>
			</div>
		);
	}

	static renderUIErrorMessage() {
		return (
			<div>
				Transitioning to loading job specs.
			</div>
		);
	}

	constructor(props: LoadingJobSpecsStateProps, context: any) {
		super(props, context);

		this.state = {
			isLoading: true,
			loadingError: null,
			apiReturnedNoJobSpecs: false,
		};
	}


	componentDidMount(): void {
		this.tryLoadSpecs();
	}

	tryLoadSpecs(): void {
		this.props.api
			.fetchJobSpecSummaries()
			.then(jobSpecs => this.onJobSpecsLoadedSuccessfully(jobSpecs))
			.catch(apiError => this.onJobSpecsLoadError(apiError));
	}

	onJobSpecsLoadedSuccessfully(jobSpecs: APIJobSpecSummary[]): void {
		const routeParams =
			Helpers.extractParams(this.props.routeProps.location.search);

		if (routeParams["based-on"] !== undefined) {
			this.setState({
				isLoading: false,
				loadingError: null,
				apiReturnedNoJobSpecs: false,
			}, () => {
				this.transitionToLoadingExistingJob(
					jobSpecs,
					routeParams["based-on"]);
			});
		} else if (jobSpecs.length === 0) {
			this.setState({
				isLoading: false,
				loadingError: null,
				apiReturnedNoJobSpecs: true,
			});
		} else {
			this.setState({
				isLoading: false,
				loadingError: null,
				apiReturnedNoJobSpecs: false,
			}, () => {
				this.transitionToLoadingFreshJob(jobSpecs);
			});
		}
	}

	transitionToLoadingExistingJob(jobSpecs: APIJobSpecSummary[], existingJobId: string): void {
		const nextComponentProps = {
			api: this.props.api,
			routeProps: this.props.routeProps,
			onTransitionRequest: this.props.onTransitionRequest,
			jobSpecs: jobSpecs,
			existingJobId: existingJobId,
		};
		const loadExistingJobComponent =
			React.createElement(LoadExistingJobRequestState, nextComponentProps, null);

		this.props.onTransitionRequest(loadExistingJobComponent);
	}

	transitionToLoadingFreshJob(jobSpecs: APIJobSpecSummary[]): void {
		const nextComponentProps: LoadingJobSpecStateProps = {
			api: this.props.api,
			routeProps: this.props.routeProps,
			onTransitionRequest: this.props.onTransitionRequest,
			jobSpecs: jobSpecs,
			partialJobRequest: LoadingJobSpecsState.createBlankPartialJobRequest(),
			specId: null,
		};
		const loadingJobSpecComponent =
			React.createElement(LoadingJobSpecState, nextComponentProps, null);

		this.props.onTransitionRequest(loadingJobSpecComponent);
	}

	onJobSpecsLoadError(apiError: APIErrorMessage): void {
		this.setState({
			isLoading: false,
			loadingError: apiError,
		});
	}

	render(): ReactElement<any> {
		if (this.state.isLoading) {
			return LoadingJobSpecsState.renderLoadingMessage();
		} else if (this.state.loadingError !== null) {
			return this.renderErrorMessage(this.state.loadingError);
		} else if (this.state.apiReturnedNoJobSpecs) {
			return LoadingJobSpecsState.renderNoJobSpecsAvailableMessage();
		} else {
			return LoadingJobSpecsState.renderUIErrorMessage();
		}
	}

	renderErrorMessage(errorMessage: APIErrorMessage): ReactElement<any> {
		return Helpers.renderAPIErrorMessage(
			"job specs",
			errorMessage,
			this.tryLoadSpecs.bind(this));
	}
}
