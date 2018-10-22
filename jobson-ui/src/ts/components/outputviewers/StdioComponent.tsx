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
import {ChangeEvent, Component, ReactElement} from "react";
import {APIErrorMessage} from "../apitypes/APIErrorMessage";
import {Observable, Subscription} from "rxjs/index";

export interface StdioComponentProps {
    fetchStdio: () => Promise<Blob>;
    onStdioUpdate: () => Observable<Blob>;
}

export interface StdioComponentState {
    isLoading: boolean;
    loadingError: null | APIErrorMessage;
    content: string;
    grepFilter: string;
}

export class StdioComponent extends Component<StdioComponentProps, StdioComponentState> {

    private static renderLoadingMessage() {
        return Helpers.renderLoadingMessage("output");
    }

    private static renderEmptyDataMessage(): ReactElement<any> {
        return (
            <div className="ui icon message">
                <i className="square outline icon"/>
                <div className="content">
                    <div className="header">
                        No content
                    </div>
                    <p>
                        This output is empty
                    </p>
                </div>
            </div>
        );
    }


    private updateSubscription: null | Subscription = null;
    private el: null | HTMLPreElement = null;


    public constructor(props: StdioComponentProps, context: any) {
        super(props, context);

        this.state = {
            isLoading: true,
            loadingError: null,
            content: "",
            grepFilter: "",
        };
    }


    public componentDidMount(): void {
        this.loadDataAndSubscribeToUpdates();
    }

    private loadDataAndSubscribeToUpdates(): void {
        this.loadData().then(this.subscribeToDataUpdates.bind(this));
    }

    private loadData(): Promise<any> {
        return this.props
            .fetchStdio()
            .then(Helpers.fetchBlobContentsAsText)
            .then(this.onLoadedStdioText.bind(this))
            .catch(this.onErrorLoadingStdioText.bind(this));
    }

    private onLoadedStdioText(text: string): void {
        this.setState({
            isLoading: false,
            loadingError: null,
            content: text,
        });
    }

    private onErrorLoadingStdioText(apiError: APIErrorMessage): void {
        if (apiError.code === 404) {
            // The output may not have been written to yet.
            this.setState({
                isLoading: false,
                loadingError: null,
                content: "",
            });
        } else {
            this.setState({
                isLoading: false,
                loadingError: apiError,
                content: "",
            });
        }
    }

    private subscribeToDataUpdates(): void {
        this.unsubscribeFromDataUpdates();

        this.updateSubscription = this.props.onStdioUpdate().subscribe(
            this.onWebsocketUpdate.bind(this),
            () => {
            });
    }

    private onWebsocketUpdate(update: Blob): void {
        Helpers.fetchBlobContentsAsText(update)
            .then(text =>
                this.setState(oldState => {
                    return {content: oldState.content + text};
                }));
    }

    private unsubscribeFromDataUpdates(): void {
        if (this.updateSubscription !== null) {
            this.updateSubscription.unsubscribe();
            this.updateSubscription = null;
        }
    }

    public componentWillUnmount(): void {
        this.unsubscribeFromDataUpdates();
    }

    public componentDidUpdate() {
        if (this.el !== null) {
            this.el.scrollTop = this.el.scrollHeight;
        }
    }


    public render(): ReactElement<any> {
        if (this.state.isLoading) {
            return StdioComponent.renderLoadingMessage();
        } else if (this.state.loadingError !== null) {
            return this.renderErrorMessage();
        } else if (this.state.content.length === 0) {
            return StdioComponent.renderEmptyDataMessage();
        } else {
            return this.renderUi();
        }
    }

    private renderErrorMessage(): ReactElement<any> {
        return Helpers.renderAPIErrorMessage(
            "output",
            this.state.loadingError,
            this.loadDataAndSubscribeToUpdates.bind(this));
    }


    private renderUi(): ReactElement<any> {
        return (
            <div>
                <div className="ui icon input">
                    <input placeholder="grep"
                           value={this.state.grepFilter}
                           onChange={this.onGrepFilterChanged.bind(this)}/>
                    <i className="search icon"/>
                </div>

                <pre ref={(el) => {
                    this.el = el
                }}>
						{this.state.grepFilter.length > 0 ?
                            this.grep(this.state.content) :
                            this.state.content}
					</pre>
            </div>
        );
    }

    private onGrepFilterChanged(e: ChangeEvent<HTMLInputElement>) {
        this.setState({grepFilter: e.target.value});
    }

    private grep(str: string): string {
        try {
            const regex = new RegExp(this.state.grepFilter);
            return str
                .split("\n")
                .filter(line => regex.test(line))
                .join("\n");
        } catch (e) {
            return str; // Incase regex mis-compiles
        }
    }
}
