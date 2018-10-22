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
import {Link} from "react-router-dom";
import {Helpers} from "../Helpers";
import {Component, FormEvent, ReactElement} from "react";
import {JobsonAPI} from "../JobsonAPI";
import {APIErrorMessage} from "./apitypes/APIErrorMessage";
import {APIJobDetails} from "./apitypes/APIJobDetails";
import {Subscription} from "rxjs/index";
import {APIJobDetailsCollection} from "./apitypes/APIJobDetailsCollection";
import {APITimestamp} from "./apitypes/APITimestamp";

export interface JobListComponentProps {
    api: JobsonAPI;
    routeProps: any;
}

export interface JobListComponentState {
    isLoadingJobs: boolean,
    jobLoadingError: null | APIErrorMessage,
    activeQuery: string,
    enteredQuery: string,
    page: number,
    jobs: APIJobDetails[],
}

export class JobListComponent extends Component<JobListComponentProps, JobListComponentState> {

    private static getLatestStatus(timestamps: APITimestamp[]) {
        return timestamps[timestamps.length - 1].status;
    }

    private static getWindowParams(routeProps: any): { [k: string]: any } {
        const params = Helpers.extractParams(routeProps.location.search);
        params.page = parseInt(params.page || "0");
        params.query = params.query || "";
        return params;
    }

    private static constructPath(page: number, query: string) {
        const start = "jobs";
        const rest = [];

        if (page > 0) rest.push(`page=${page}`);
        if (query.length > 0) rest.push(`query=${query}`);

        return rest.length === 0 ?
            start :
            start + "?" + rest.join("&");
    }

    private static renderLoadingMessage(): ReactElement<any> {
        return Helpers.renderLoadingMessage("jobs");
    }

    private static renderUserHasNoJobsMessage(): ReactElement<any> {
        return (
            <div className="ui info icon message">
                <i className="info circle icon"/>
                <div className="content">
                    <div className="header">
                        No jobs yet!
                    </div>
                    <p>
                        You don't seem to have any jobs yet
                        , <Link className="ui primary button" to="/submit">
                        Submit your first job
                    </Link>
                    </p>
                </div>
            </div>
        );
    }

    private static renderNoResultsMessage(): ReactElement<any> {
        return (
            <div className="ui negative icon message">
                <i className="warning icon"/>
                <div className="content">
                    <div className="header">
                        Your search returned no results
                    </div>
                </div>
            </div>
        );
    }


    private jobUpdatesSubscription: Subscription | null;


    public constructor(props: JobListComponentProps, context: any) {
        super(props, context);

        const params = JobListComponent.getWindowParams(props.routeProps);

        this.state = {
            isLoadingJobs: true,
            jobLoadingError: null,
            activeQuery: params.query,
            enteredQuery: params.query,
            page: params.page,
            jobs: [],
        };
    }

    public componentWillMount(): void {
        this.initializeJobUpdatesSubscription();
        this.updateJobList();
    }

    private initializeJobUpdatesSubscription(): void {
        this.jobUpdatesSubscription = this.props.api.onAllJobStatusChanges()
            .subscribe(
                () => this.updateJobList(),
                () => {
                }, // Swallow this for now - no feedback point in UI
                () => {
                });
    }

    private updateJobList(): void {
        const stateUpdate = {
            isLoadingJobs: true,
        };

        const afterStateUpdate = () => {
            this.props.api.fetchJobSummaries(this.state.activeQuery, this.state.page)
                .then(this.onJobSummariesLoaded.bind(this))
                .catch(this.onJobSummariesLoadError.bind(this))
        };

        this.setState(stateUpdate, afterStateUpdate);
    }

    private onJobSummariesLoaded(page: APIJobDetailsCollection): void {
        this.setState({
            isLoadingJobs: false,
            jobLoadingError: null,
            jobs: page.entries,
        });
    }

    private onJobSummariesLoadError(error: APIErrorMessage): void {
        this.setState({
            isLoadingJobs: false,
            jobLoadingError: error,
        });
    }

    public componentWillReceiveProps(newProps: JobListComponentProps): void {
        const params = JobListComponent.getWindowParams(newProps.routeProps);

        if (params.page != this.state.page ||
            params.query != this.state.activeQuery) {

            this.setState({
                page: params.page,
                activeQuery: params.query,
                enteredQuery: params.query,
            }, this.updateJobList.bind(this));
        }
    }

    public componentWillUnmount(): void {
        if (this.jobUpdatesSubscription !== null) {
            this.jobUpdatesSubscription.unsubscribe();
        }
    }

    private onSearchInputChange(e: FormEvent<HTMLInputElement>): void {
        this.setState({
            enteredQuery: e.currentTarget.value,
        });
    }

    private onSearchKeyUp(e: KeyboardEvent): void {
        if (e.key === "Enter" && this.state.enteredQuery !== this.state.activeQuery) {
            this.pushHistory(0, this.state.enteredQuery);
        }
    }

    private pushHistory(page: number, query: string): void {
        this.props.routeProps.history.push(JobListComponent.constructPath(page, query));
    }

    private onClickedNextPage(): void {
        this.pushHistory(this.state.page + 1, this.state.activeQuery);
    }

    private onClickedPreviousPage(): void {
        this.pushHistory(this.state.page - 1, this.state.activeQuery);
    }

    private userHasNoJobs(): boolean {
        return !this.state.isLoadingJobs &&
            this.state.jobs.length === 0 &&
            this.state.activeQuery.length === 0 &&
            this.state.page === 0;
    }

    private isLoadingJobs(): boolean {
        return this.state.isLoadingJobs;
    }

    private errorLoadingJobs(): boolean {
        return this.state.jobLoadingError !== null;
    }

    public render(): ReactElement<any> {
        return (
            <div>
                {this.renderSearchBar()}
                {this.renderMainArea()}
            </div>
        );
    }

    private renderSearchBar(): ReactElement<any> {
        return (
            <div className={"ui fluid left icon input " + (this.isLoadingJobs() ? "loading" : "")}
                 style={{marginBottom: "2em"}}>
                <i className="search icon"/>
                <input type="text"
                       id="jobs-search"
                       placeholder="Search jobs..."
                       onChange={this.onSearchInputChange.bind(this)}
                       onKeyUp={this.onSearchKeyUp.bind(this)}
                       value={this.state.enteredQuery}
                       autoFocus
                       disabled={this.userHasNoJobs() || this.isLoadingJobs() || this.errorLoadingJobs()}/>
            </div>
        );
    }

    private renderMainArea(): ReactElement<any> {
        if (this.state.isLoadingJobs) {
            return JobListComponent.renderLoadingMessage();
        } else if (this.state.jobLoadingError !== null) {
            return this.renderLoadingErrorMessage();
        } else if (this.userHasNoJobs()) {
            return JobListComponent.renderUserHasNoJobsMessage();
        } else if (this.state.jobs.length === 0) {
            return JobListComponent.renderNoResultsMessage();
        } else {
            return this.renderJobSummaries();
        }
    }

    private renderLoadingErrorMessage(): ReactElement<any> {
        return Helpers.renderAPIErrorMessage(
            "jobs",
            this.state.jobLoadingError,
            this.updateJobList.bind(this));
    }

    private renderJobSummaries(): ReactElement<any> {
        const renderJobSummary = this.renderJobSummary.bind(this);

        return (
            <div>
                <table id="job-list" className="ui very basic table">
                    <thead>
                    <tr>
                        <th className="center aligned">ID</th>
                        <th className="center aligned">Owner</th>
                        <th className="center aligned">Name</th>
                        <th className="center aligned">Status</th>
                        <th className="center aligned">Actions</th>
                    </tr>
                    </thead>

                    <tbody>
                    {this.state.jobs.map(renderJobSummary)}
                    </tbody>
                </table>

                <div style={{textAlign: "center"}}>
                    <button className="ui left attached button"
                            disabled={this.state.page === 0}
                            onClick={this.onClickedPreviousPage.bind(this)}>
                        Newer Jobs
                    </button>
                    <button className="ui right attached button"
                            onClick={this.onClickedNextPage.bind(this)}>
                        Older Jobs
                    </button>
                </div>
            </div>
        );
    }

    private renderJobSummary(jobSummary: APIJobDetails, i: number): ReactElement<any> {
        return (
            <tr key={i}>
                <td className="center aligned">
                    <Link to={"/jobs/" + jobSummary.id}>
                        <code>{jobSummary.id}</code>
                    </Link>
                </td>
                <td className="center aligned">
                    {jobSummary.owner}
                </td>
                <td className="center aligned">
                    {jobSummary.name}
                </td>
                <td className="center aligned">
                    {Helpers.renderStatusField(
                        JobListComponent.getLatestStatus(jobSummary.timestamps))}
                </td>
                <td className="center aligned">
                    {this.renderJobActions(jobSummary)}
                </td>
            </tr>
        );
    }

    private renderJobActions(jobSummary: APIJobDetails): ReactElement<any>[] {
        return Helpers.renderAllJobActions(this.props.api, jobSummary.id, jobSummary._links);
    }
}
