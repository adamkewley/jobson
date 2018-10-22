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
import {NavLink} from "react-router-dom";
import {JobsonAPI} from "../JobsonAPI";
import {ReactElement} from "react";

export interface NavbarComponentProps {
    api: JobsonAPI,
}

export interface NavbarComponentState {
    isLoadingUsername: boolean,
    errorLoadingUsername: boolean,
    username: null | string,
}

export class NavbarComponent extends React.Component<NavbarComponentProps, NavbarComponentState> {

    private static renderLoadingIcon(): ReactElement<any> {
        return (
            <div className="ui tiny active inline loader">
            </div>
        );
    }

    private static renderErrorTag(): ReactElement<any> {
        return (
            <div className="ui tiny red horizontal basic label">
                Error loading username
            </div>
        );
    }

    private static renderUsername(username: string): ReactElement<any> {
        return (
            <div>
                <i className="user icon"/>
                {username}
            </div>
        );
    }


    public constructor(props: NavbarComponentProps, context: any) {
        super(props, context);

        this.state = {
            isLoadingUsername: true,
            errorLoadingUsername: false,
            username: null,
        };
    }


    public componentDidMount(): void {
        this.loadCurrentUserName();
    }

    private loadCurrentUserName(): void {
        const stateUpdate: NavbarComponentState = {
            isLoadingUsername: true,
            errorLoadingUsername: false,
            username: null,
        };

        const afterUpdate = () => {
            this.props.api.fetchCurrentUser()
                .then(this.onUserLoaded.bind(this))
                .catch(this.onUserLoadError.bind(this));
        };

        this.setState(stateUpdate, afterUpdate);
    }

    private onUserLoaded(userId: string): void {
        this.setState({
            isLoadingUsername: false,
            username: userId,
        });
    }

    private onUserLoadError(): void {
        this.setState({
            isLoadingUsername: false,
            errorLoadingUsername: true,
            username: null,
        });
    }

    public render(): ReactElement<any> {
        return (
            <div className="ui secondary pointing menu">
                <div className="ui container">

					<span className="header item">
						Jobson
					</span>

                    <NavLink to="/jobs" className="item" activeClassName="active">
                        Jobs
                    </NavLink>
                    <NavLink to="/submit" className="item" activeClassName="active">
                        Submit Job
                    </NavLink>

                    <div className="right menu">
                        <em className="item">
                            {this.tryRenderUser()}
                        </em>
                    </div>
                </div>
            </div>
        );
    }

    private tryRenderUser(): ReactElement<any> {
        if (this.state.isLoadingUsername) {
            return NavbarComponent.renderLoadingIcon();
        } else if (this.state.errorLoadingUsername) {
            return NavbarComponent.renderErrorTag();
        } else {
            return NavbarComponent.renderUsername(this.state.username);
        }
    }
}
