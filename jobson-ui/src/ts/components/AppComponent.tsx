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

import * as React from 'react';
import {HttpService} from "../HttpService";
import {JobsonAPI} from "../JobsonAPI";
import {JobListComponent} from "./JobListComponent";
import {JobDetailsComponent} from "./JobDetailsComponent";
import {Redirect, Route, Switch} from "react-router";
import {NavbarComponent} from "./NavbarComponent";
import {SubmitJobComponent} from "./SubmitJobComponent";
import {APIErrorMessage} from "./apitypes/APIErrorMessage";
import {Component} from "react";

export interface AppComponentProps {
}

export interface AppComponentState {
    httpService: HttpService,
    configLoading: boolean,
    configLoadingError: null | APIErrorMessage,
    api: null | JobsonAPI,
    requests: XMLHttpRequest[]
}

export class AppComponent extends Component<AppComponentProps, AppComponentState> {

    static renderConfigLoadingScreen() {
        return (
            <div className="ui active dimmer">
                <div className="ui text loader">
                    Loading configuration
                </div>
            </div>
        );
    }

    static renderFooter() {
        return (
            <div className="ui inverted vertical footer segment">
                <div className="ui container">
                    <div className="ui stackable inverted divided equal height stackable grid">
                        <div className="ten wide column">
                            <h4 className="ui inverted header">Jobson UI</h4>
                            <p>
                                Jobson is an open-source web server that can turn command-line
                                applications into a job system.
                            </p>
                        </div>
                        <div className="six wide column">
                            <div className="ui inverted link list">
                                <a className="item" href="https://github.com/adamkewley/jobson">Jobson Project</a>
                                <a className="item" href="https://github.com/adamkewley/jobson-ui">Jobson UI Project</a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }


    constructor(props: AppComponentProps, context: any) {
        super(props, context);

        const httpService = new HttpService();

        this.state = {
            httpService: httpService,
            configLoading: true,
            configLoadingError: null,
            api: null,
            requests: [],
        };
    }

    componentWillMount() {
        this.initializeLoadingBar();
        this.loadConfig();
    }

    initializeLoadingBar() {
        this.state.httpService.onRequestsChanged.subscribe(requests => {
            this.setState({requests: requests});
        });
    }

    loadConfig() {
        this.setState({
            configLoading: true,
            configLoadingError: null,
            api: null,
        }, () => {
            this.state.httpService.get("config.json")
                .then(config => {
                    this.setState({
                        configLoading: false,
                        configLoadingError: null,
                        api: new JobsonAPI(this.state.httpService, JSON.parse(config).apiPrefix),
                    });
                })
                .catch(err => {
                    this.setState({
                        configLoading: false,
                        configLoadingError: err,
                        api: null,
                    });
                });
        });
    }

    render() {
        if (this.state.configLoading) {
            return AppComponent.renderConfigLoadingScreen();
        } else if (this.state.configLoadingError !== null) {
            return this.renderConfigLoadingErrorScreen(this.state.configLoadingError);
        } else {
            return this.renderConfigLoadedScreen();
        }
    }

    renderConfigLoadingErrorScreen(configLoadingError: APIErrorMessage) {
        return (
            <div className="ui container">
                <div className="ui secondary pointing menu">
                    <div className="ui container">

					<span className="header item">
						Jobson
					</span>
                    </div>
                </div>

                <div className="ui negative icon message">
                    <i className="warning circle icon"/>
                    <div className="content">
                        <div className="header">
                            <h1>Error Loading UI Configuration</h1>
                        </div>

                        <p>
                            The Jobson UI configuration (<a href="config.json">config.json</a>) could not be loaded. The
                            UI tried to
                            fetch the configuration (via a <code>GET</code> request for <code>config.json</code>).
                            However, the server
                            responded with &quot;{configLoadingError.message}&quot; (HTTP
                            code: {configLoadingError.code}).
                        </p>

                        <button className="ui primary icon button"
                                onClick={this.loadConfig.bind(this)}>
                            <i className="refresh icon"/>
                            Try Again
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    renderConfigLoadedScreen() {
        return (
            <div>
                {this.state.requests.length > 0 ?
                    <div className="loading-bar enabled"/> :
                    <div className="loading-bar"/>}

                <div id="root-container">
                    <div>
                        {this.state.api !== null ? <NavbarComponent api={this.state.api}/> : null}

                        {this.renderMain()}

                        {AppComponent.renderFooter()}
                    </div>
                </div>
            </div>
        );
    }

    renderMain() {
        return (
            <main className="ui container" style={{marginBottom: "1em"}}>
                <Switch>
                    <Route path="/submit"
                           render={props => <SubmitJobComponent api={this.state.api} routeProps={props}/>}/>
                    <Route path="/jobs/:id"
                           render={props => <JobDetailsComponent params={props.match.params} api={this.state.api}/>}/>
                    <Route path="/jobs"
                           render={props => <JobListComponent api={this.state.api} routeProps={props}/>}/>
                    <Redirect from={"/"} to={"/jobs"}/>
                </Switch>
            </main>
        );
    }
}
