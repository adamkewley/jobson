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
import {Component, ReactElement} from "react"
import {Helpers} from "../../Helpers";
import {APIErrorMessage} from "../apitypes/APIErrorMessage";
import {APIJobSpec} from "../apitypes/APIJobSpec";
import {JobsonAPI} from "../../JobsonAPI";
import {APIExpectedInput} from "../apitypes/APIExpectedInput";

export interface SpecViewerComponentProps {
    api: JobsonAPI;
    specUrl: string;
    jobId: string;
}

export interface SpecViewerComponentState {
    isLoadingJobSpec: boolean;
    loadingError: null | APIErrorMessage;
    jobSpec: null | APIJobSpec;
}

export class SpecViewerComponent extends Component<SpecViewerComponentProps, SpecViewerComponentState> {

    private static renderLoadingMessage() {
        return Helpers.renderLoadingMessage("job spec");
    }

    private static renderProp(name: string, description: string, i: number): ReactElement<any> {
        return (
            <div className="item" key={i}>
                <div className="header">{name}</div>
                <div className="description">
                    {description}
                </div>
            </div>
        );
    }

    private static renderJobHasNoExpectedInputs(): ReactElement<any> {
        return <div>This spec has no expected inputs</div>;
    }

    private static renderJobExpectedInput(expectedInput: APIExpectedInput, i: number): ReactElement<any> {
        return SpecViewerComponent.renderProp(expectedInput.id, expectedInput.type, i);
    }


    public constructor(props: SpecViewerComponentProps, context: any) {
        super(props, context);

        this.state = {
            isLoadingJobSpec: true,
            loadingError: null,
            jobSpec: null,
        };
    }

    public componentDidMount(): void {
        this.loadJobSpec();
    }

    private loadJobSpec(): void {
        this.props.api.fetchJsonResource(this.props.specUrl)
            .then(this.onLoadedJobSpec.bind(this))
            .catch(this.onJobSpecLoadError.bind(this));
    }

    private onLoadedJobSpec(jobSpec: APIJobSpec): void {
        this.setState({
            isLoadingJobSpec: false,
            loadingError: null,
            jobSpec: jobSpec,
        });
    }

    private onJobSpecLoadError(error: APIErrorMessage): void {
        this.setState({
            isLoadingJobSpec: false,
            loadingError: error,
            jobSpec: null,
        });
    }

    public render(): ReactElement<any> {
        if (this.state.isLoadingJobSpec) {
            return SpecViewerComponent.renderLoadingMessage();
        } else if (this.state.loadingError !== null) {
            return this.renderErrorMessage();
        } else {
            return this.renderViewer();
        }
    }

    private renderErrorMessage(): ReactElement<any> {
        return Helpers.renderAPIErrorMessage(
            "job spec",
            this.state.loadingError,
            this.loadJobSpec.bind(this));
    }

    private renderViewer(): ReactElement<any> {
        return (
            <div className="ui grid jobson-condensed-grid">
                <div className="twelve wide column">
                    <h3 className="header">
                        Spec Properties
                    </h3>
                </div>

                <div className="four wide column">
                    <div className="ui right floated">
                        {Helpers.renderDownloadButton(this.props.specUrl)}
                    </div>
                </div>

                <div className="sixteen wide column">
                    <div className="ui list">
                        {SpecViewerComponent.renderProp("id", this.state.jobSpec.id, 0)}
                        {SpecViewerComponent.renderProp("name", this.state.jobSpec.name, 1)}
                        {SpecViewerComponent.renderProp("description", this.state.jobSpec.description, 2)}
                    </div>

                    <h3>Expected Inputs</h3>
                    {this.state.jobSpec.expectedInputs.length > 0 ?
                        this.renderExpectedInputs() :
                        SpecViewerComponent.renderJobHasNoExpectedInputs()}
                </div>
            </div>
        );
    }

    private renderExpectedInputs(): ReactElement<any> {
        return (
            <div className="ui list">
                {this.state.jobSpec.expectedInputs.map(SpecViewerComponent.renderJobExpectedInput)}
            </div>
        );
    }
}
