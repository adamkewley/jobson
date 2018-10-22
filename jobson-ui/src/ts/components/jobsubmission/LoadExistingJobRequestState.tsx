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
import {Helpers} from "../../Helpers";
import {EditingJobRequestState} from "./EditingJobRequestState";
import {JobsonAPI} from "../../JobsonAPI";
import {APIJobSpecSummary} from "../apitypes/APIJobSpecSummary";
import {ReactElement} from "react";
import {APIJobDetails} from "../apitypes/APIJobDetails";
import {APIErrorMessage} from "../apitypes/APIErrorMessage";
import {APIJobSpec} from "../apitypes/APIJobSpec";

export interface LoadExistingJobRequestStateProps {
    api: JobsonAPI;
    routeProps: any;
    onTransitionRequest: (el: ReactElement<any>) => void;
    jobSpecs: APIJobSpecSummary[];
    existingJobId: string;
}

export interface LoadExistingJobRequestStateState {
    jobDetails: null | APIJobDetails;
    jobLoadingError: null | APIErrorMessage;
    jobInputs: null | { [inputName: string]: any };
    jobInputsLoadingError: null | APIErrorMessage;
    existingSpec: null | APIJobSpec;
    existingSpecLoadError: null | APIErrorMessage;
    currentSpec: null | APIJobSpec;
    currentSpecLoadError: null | APIErrorMessage;
}

/**
 * State that occurs when specs are loaded but the client
 * specified they want to base the job request on an existing
 * request (which requires loading the existing req, spotting
 * spec changes, coercing inputs, etc.).
 */
export class LoadExistingJobRequestState extends React.Component<LoadExistingJobRequestStateProps, LoadExistingJobRequestStateState> {

    private static renderLoadingJobDetailsMessage(): ReactElement<any> {
        return Helpers.renderLoadingMessage("job details");
    }

    private static createNullState(): LoadExistingJobRequestStateState {
        return {
            jobDetails: null,
            jobLoadingError: null,
            jobInputs: null,
            jobInputsLoadingError: null,
            existingSpec: null,
            existingSpecLoadError: null,
            currentSpec: null,
            currentSpecLoadError: null,
        };
    }


    public constructor(props: LoadExistingJobRequestStateProps, context: any) {
        super(props, context);

        this.state = LoadExistingJobRequestState.createNullState();
    }

    public componentDidMount(): void {
        this.tryLoadExistingJob();
    }

    private tryLoadExistingJob(): void {
        this.setState(LoadExistingJobRequestState.createNullState(), () => {
            this.props.api
                .fetchJobDetailsById(this.props.existingJobId)
                .then(this.onLoadedExistingJob.bind(this))
                .catch(this.onErrorLoadingExistingJob.bind(this));
        });
    }

    private onLoadedExistingJob(jobDetails: APIJobDetails): void {
        this.setState({jobDetails}, () => {
            this.props.api.fetchJobInputs(this.props.existingJobId)
                .then(this.onLoadedJobInputs.bind(this))
                .catch(this.onErrorLoadingJobInputs.bind(this));
        });
    }

    private onErrorLoadingExistingJob(jobLoadingError: APIErrorMessage): void {
        this.setState({jobLoadingError});
    }

    private onLoadedJobInputs(jobInputs: { [inputName: string]: any }): void {
        this.setState({jobInputs}, () => {
            this.props.api.fetchExistingJobsSpec(this.props.existingJobId)
                .then(this.onLoadedExistingJobSpec.bind(this))
                .catch(this.onErrorLoadingExistingJobSpec.bind(this));
        });
    }

    private onErrorLoadingJobInputs(jobInputsLoadingError: APIErrorMessage): void {
        this.setState({jobInputsLoadingError});
    }

    private onLoadedExistingJobSpec(existingSpec: APIJobSpec) {
        this.setState({existingSpec}, () => {
            this.props.api.fetchJobSpec(existingSpec.id)
                .then(this.onLoadedCurrentJobSpec.bind(this))
                .catch(this.onErrorLoadingCurrentJobSpec.bind(this));
        });
    }

    private onErrorLoadingExistingJobSpec(existingSpecLoadError: APIErrorMessage): void {
        this.setState({existingSpecLoadError});
    }

    private onLoadedCurrentJobSpec(currentSpec: APIJobSpec): void {
        this.setState({currentSpec}, () => {
            const copiedJobRequest = {
                spec: this.state.existingSpec.id,
                name: this.state.jobDetails.name,
                inputs: this.state.jobInputs,
            };

            const nextComponentProps = {
                api: this.props.api,
                routeProps: this.props.routeProps,
                onTransitionRequest: this.props.onTransitionRequest,
                jobSpecs: this.props.jobSpecs,
                partialJobRequest: copiedJobRequest,
                jobSpec: this.state.currentSpec,
            };

            const jobEditorComponent =
                React.createElement(EditingJobRequestState, nextComponentProps, null);


            this.props.onTransitionRequest(jobEditorComponent);
        });
    }

    private onErrorLoadingCurrentJobSpec(currentSpecLoadError: APIErrorMessage): void {
        this.setState({currentSpecLoadError});
    }

    public render(): ReactElement<any> {
        if (this.state.jobLoadingError) {
            return this.renderErrorLoadingJobDetailsMessage();
        } else if (this.state.jobDetails === null) {
            return LoadExistingJobRequestState.renderLoadingJobDetailsMessage();
        } else if (this.state.jobInputsLoadingError) {
            return this.renderErrorLoadingJobInputs();
        } else if (this.state.existingSpecLoadError) {
            return this.renderErrorLoadingExistingSpec();
        } else if (this.state.currentSpecLoadError) {
            return this.renderErrorLoadingCurrentSpec();
        } else {
            return this.renderPopulatingJob();
        }
    }

    private renderErrorLoadingJobDetailsMessage() {
        return Helpers.renderAPIErrorMessage(
            "loading job details",
            this.state.jobLoadingError,
            () => this.tryLoadExistingJob());
    }

    private renderErrorLoadingJobInputs() {
        return Helpers.renderAPIErrorMessage(
            "loading job inputs",
            this.state.jobInputsLoadingError,
            () => this.tryLoadExistingJob());
    }

    private renderErrorLoadingExistingSpec() {
        return Helpers.renderAPIErrorMessage(
            "loading existing spec",
            this.state.existingSpecLoadError,
            () => this.tryLoadExistingJob());
    }

    private renderErrorLoadingCurrentSpec() {
        return Helpers.renderAPIErrorMessage(
            "loading current version of spec",
            this.state.currentSpecLoadError,
            () => this.tryLoadExistingJob());
    }

    private renderPopulatingJob() {
        return Helpers.renderLoadingMessage(`populating UI with ${this.props.existingJobId}'s details`);
    }
}
