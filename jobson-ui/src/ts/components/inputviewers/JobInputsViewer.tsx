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
import ReactJson from 'react-json-view';
import {Helpers} from "../../Helpers";
import {Component, ReactElement} from "react";
import {JobsonAPI} from "../../JobsonAPI";
import {APIErrorMessage} from "../apitypes/APIErrorMessage";

export interface JobInputsViewerProps {
	jobId: string;
	api: JobsonAPI;
}

export interface JobInputsViewerState {
	isLoadingJobInputs: boolean;
	loadingError: null | APIErrorMessage;
	inputs: null | {[inputName: string]: any};
}

export class JobInputsViewer extends Component<JobInputsViewerProps, JobInputsViewerState> {

    private static renderLoadingMessage(): ReactElement<any> {
        return Helpers.renderLoadingMessage("inputs");
    }

    private static canBeRenderedInABrowser(inputs: {[inputName: string]: any}) {
        return JSON.stringify(inputs).length < 200000;
    }


	public constructor(props: JobInputsViewerProps, context: any) {
		super(props, context);

		this.state = {
			isLoadingJobInputs: true,
			loadingError: null,
			inputs: null,
		};
	}


	public componentDidMount(): void {
		this.loadInputs();
	}

	private loadInputs(): void {
		this.props.api.fetchJobInputs(this.props.jobId)
			.then(this.onLoadedInputs.bind(this))
			.catch(this.onErrorLoadingInputs.bind(this));
	}

	private onLoadedInputs(inputs: {[inputName: string]: any}): void {
        this.setState({
            isLoadingJobInputs: false,
            loadingError: null,
            inputs: inputs,
        });
	}

	private onErrorLoadingInputs(apiError: APIErrorMessage): void {
        this.setState({
            isLoadingJobInputs: false,
            loadingError: apiError,
            inputs: null,
        });
	}

	public render(): ReactElement<any> {
		if (this.state.isLoadingJobInputs) {
            return JobInputsViewer.renderLoadingMessage();
        } else if (this.state.loadingError !== null) {
            return this.renderErrorMessage();
        } else {
            return this.renderInputs();
		}
	}

	private renderErrorMessage(): ReactElement<any> {
		return Helpers.renderAPIErrorMessage(
			"inputs",
			this.state.loadingError,
			this.loadInputs.bind(this));
	}

	private renderInputs(): ReactElement<any> {
		return (
			<div className="ui grid jobson-condensed-grid">
				<div className="twelve wide column">
					<h3 className="header">
						inputs
					</h3>
				</div>
				<div className="four wide column">
					{Helpers.renderDownloadButton(this.props.api.urlToGetJobInputs(this.props.jobId))}
				</div>

				<div className="sixteen wide column">
					{JobInputsViewer.canBeRenderedInABrowser(this.state.inputs) ?
						this.renderJSONViewer() :
						this.renderInputsTooBigToViewMessage()}
				</div>
			</div>
		);
	}

	private renderJSONViewer(): ReactElement<any> {
		return (
				<ReactJson src={this.state.inputs}
						   name={null}
						   theme="monokai"
						   displayDataTypes={false}/>
		);
	}

	private renderInputsTooBigToViewMessage(): ReactElement<any> {
		return (
			<div className="ui icon warning message">
				<i className="warning icon"/>
				<div className="content">
					<div className="header">
						Cannot Display Inputs
					</div>
					<p>
						The inputs used in this job are too big to show in the browser.
						You can try viewing them in with your own software by downloading
						them.
					</p>
					{Helpers.renderDownloadButton(this.props.api.urlToGetJobInputs(this.props.jobId))}
				</div>
			</div>
		);
	}
}
