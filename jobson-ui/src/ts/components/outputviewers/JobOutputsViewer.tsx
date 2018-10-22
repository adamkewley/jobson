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
import {StdioComponent} from "./StdioComponent";
import {Helpers} from "../../Helpers";
import {Component, ReactElement} from "react";
import {Observable, Subscription} from "rxjs/index";
import {APIJobEvent} from "../apitypes/APIJobEvent";
import {JobsonAPI} from "../../JobsonAPI";
import {APIErrorMessage} from "../apitypes/APIErrorMessage";
import {APIJobOutput} from "../apitypes/APIJobOutput";

export interface JobOutputsViewerProps {
    jobChangesSubject: Observable<APIJobEvent>;
    jobId: string;
    api: JobsonAPI;
}

export interface JobOutputsViewerState {
    isLoadingJobOutputs: boolean;
    loadingError: null | APIErrorMessage;
    jobOutputs: null | APIJobOutput[];
}

export interface RenderJobOutputArgs {
    title: string;
    description: null | string;
    downloadHref: string;
    viewer: ReactElement<any>;
}

export class JobOutputsViewer extends Component<JobOutputsViewerProps, JobOutputsViewerState> {

    private static renderLoadingMessage() {
        return Helpers.renderLoadingMessage("job outputs");
    }

    private static renderStdioOutput(title: string,
                                     href: string,
                                     fetchStdio: () => Promise<Blob>,
                                     onStdioUpdate: () => Observable<Blob>): ReactElement<any> {

        const stdioViewer = <StdioComponent fetchStdio={fetchStdio} onStdioUpdate={onStdioUpdate}/>;

        return JobOutputsViewer.renderJobOutput({
            title: title,
            description: null,
            downloadHref: href,
            viewer: stdioViewer
        });
    }

    private static renderJobOutput(args: RenderJobOutputArgs) {
        return (
            <div className="ui grid jobson-condensed-grid" key={args.downloadHref}>
                <div className="twelve wide column">
                    <h3 className="header">
                        {args.title}
                    </h3>
                    {args.description}
                </div>

                <div className="four wide column">
                    <div className="ui right floated">
                        {Helpers.renderDownloadButton(args.downloadHref)}
                    </div>
                </div>

                <div className="sixteen wide column">
                    {args.viewer}
                    <div className="ui divider"/>
                </div>
            </div>
        );
    }


    private jobChangesSubscription: null | Subscription;


    public constructor(props: JobOutputsViewerProps, context: any) {
        super(props, context);

        this.jobChangesSubscription = null;

        this.state = {
            isLoadingJobOutputs: true,
            loadingError: null,
            jobOutputs: null,
        };
    }


    public componentDidMount(): void {
        this.loadJobOutputs();
        this.createSubscriptionToJobChanges();
    }

    private createSubscriptionToJobChanges(): void {
        this.jobChangesSubscription =
            this.props.jobChangesSubject.subscribe(
                () => this.loadJobOutputs(),
                () => {
                }); // Swallowed because the top-level job details component will show feedback)
    }

    public componentWillUnmount(): void {
        this.unsubscribeFromJobChanges();
    }

    private unsubscribeFromJobChanges(): void {
        if (this.jobChangesSubscription !== null) {
            this.jobChangesSubscription.unsubscribe();
            this.jobChangesSubscription = null;
        }
    }

    private loadJobOutputs(): void {
        this.props.api.fetchJobOutputs(this.props.jobId)
            .then(this.onLoadedJobOutputs.bind(this))
            .catch(this.onErrorLoadingJobOutputs.bind(this));
    }

    private onLoadedJobOutputs(jobOutputs: APIJobOutput[]): void {
        this.setState({
            isLoadingJobOutputs: false,
            loadingError: null,
            jobOutputs: jobOutputs,
        });
    }

    private onErrorLoadingJobOutputs(apiError: APIErrorMessage): void {
        this.setState({
            isLoadingJobOutputs: false,
            loadingError: apiError,
            jobOutputs: null,
        });
    }

    public render(): ReactElement<any> {
        if (this.state.isLoadingJobOutputs) {
            return JobOutputsViewer.renderLoadingMessage();
        } else if (this.state.loadingError !== null) {
            return this.renderErrorMessage();
        } else {
            return this.renderOutputsUi();
        }
    }

    private renderErrorMessage(): ReactElement<any> {
        return Helpers.renderAPIErrorMessage(
            "job outputs",
            this.state.loadingError,
            this.loadJobOutputs.bind(this));
    }

    private renderOutputsUi(): ReactElement<any> {
        return (
            <div>
                {this.renderFileOutputs()}
                {JobOutputsViewer.renderStdioOutput(
                    "stdout",
                    this.props.api.urlToGetJobStdout(this.props.jobId),
                    () => this.props.api.fetchJobStdout(this.props.jobId),
                    () => this.props.api.onJobStdoutUpdate(this.props.jobId))}
                {JobOutputsViewer.renderStdioOutput(
                    "stderr",
                    this.props.api.urlToGetJobStderr(this.props.jobId),
                    () => this.props.api.fetchJobStderr(this.props.jobId),
                    () => this.props.api.onJobStderrUpdate(this.props.jobId))}
            </div>
        );
    }

    private renderFileOutputs(): ReactElement<any>[] {
        return this.state.jobOutputs.map((output) => {
            return this.renderFileOutput(output);
        });
    }

    private renderFileOutput(jobOutput: APIJobOutput): ReactElement<any> {
        const viewer =
            (jobOutput.metadata && jobOutput.metadata.embed) ?
                <embed className="ui image" src={this.props.api.buildAPIPathTo(jobOutput.href)}/> :
                null;

        return JobOutputsViewer.renderJobOutput({
            title: jobOutput.name || jobOutput.id,
            description: jobOutput.description || null,
            downloadHref: this.props.api.buildAPIPathTo(jobOutput.href),
            viewer: viewer,
        });
    }
}
