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
import {Component, ReactElement} from "react";
import {APIJobSpecSummary} from "../apitypes/APIJobSpecSummary";
import {APIErrorMessage} from "../apitypes/APIErrorMessage";
import {APIJobSpec} from "../apitypes/APIJobSpec";

export interface LoadingJobSpecStateProps {
    api: JobsonAPI;
    routeProps: any;
    onTransitionRequest: (el: ReactElement<any>) => void;
    jobSpecs: APIJobSpecSummary[];
    partialJobRequest: any;
    specId: string | null;
}

export interface LoadingJobSpecStateState {
    selectedSpecId: string;
    isLoading: boolean;
    loadingError: null | APIErrorMessage;
}

/**
 * State that occurs whenever the client needs to load a
 * full job spec from the API.
 */
export class LoadingJobSpecState extends Component<LoadingJobSpecStateProps, LoadingJobSpecStateState> {

    private static renderLoadingMessage() {
        return Helpers.renderLoadingMessage("spec");
    }

    private static renderUIError() {
        return (
            <div>
                The UI entered an invalid state (spec loaded, no error,
                but didn't transition to the editor). This is a bug.
            </div>
        );
    }


    public constructor(props: LoadingJobSpecStateProps, context: any) {
        super(props, context);

        const routeParams = Helpers.extractParams(this.props.routeProps.location.search);

        this.state = {
            selectedSpecId: props.specId || routeParams["spec"] || props.jobSpecs[0].id,
            isLoading: true,
            loadingError: null,
        };
    }


    public componentDidMount(): void {
        this.tryLoadJobSpec(this.state.selectedSpecId);
    }

    private tryLoadJobSpec(specId: string): void {
        const stateUpdate: LoadingJobSpecStateState = {
            selectedSpecId: specId,
            isLoading: true,
            loadingError: null,
        };

        const afterUpdate = () => {
            this.props.api.fetchJobSpec(specId)
                .then(jobSpec => this.onJobSpecLoaded(jobSpec))
                .catch(apiError => this.onJobSpecLoadError(apiError));
        };

        this.setState(stateUpdate, afterUpdate);
    }

    private onJobSpecLoaded(jobSpec: APIJobSpec): void {
        const updatedJobRequest =
            Object.assign({}, this.props.partialJobRequest, {spec: jobSpec.id, inputs: {}});

        const nextComponentProps = {
            api: this.props.api,
            routeProps: this.props.routeProps,
            onTransitionRequest: this.props.onTransitionRequest,
            jobSpecs: this.props.jobSpecs,
            partialJobRequest: updatedJobRequest,
            jobSpec: jobSpec,
        };

        const jobEditorComponent = React.createElement(EditingJobRequestState, nextComponentProps, null);

        this.props.onTransitionRequest(jobEditorComponent);
    }

    private onJobSpecLoadError(apiError: APIErrorMessage): void {
        this.setState({
            isLoading: false,
            loadingError: apiError,
        });
    }

    public render(): ReactElement<any> {
        if (this.state.isLoading) {
            return LoadingJobSpecState.renderLoadingMessage();
        } else if (this.state.loadingError) {
            return this.renderLoadingError();
        } else {
            return LoadingJobSpecState.renderUIError();
        }
    }

    private renderLoadingError(): ReactElement<any> {
        return Helpers.renderAPIErrorMessage(
            "spec",
            this.state.loadingError,
            () => this.tryLoadJobSpec(this.state.selectedSpecId));
    }
}
