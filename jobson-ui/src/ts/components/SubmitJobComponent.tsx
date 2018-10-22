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
import {ReactElement} from "react";
import {LoadingJobSpecsState} from "./jobsubmission/LoadingJobSpecsState";
import {JobsonAPI} from "../JobsonAPI";

export interface SubmitJobComponentProps {
    api: JobsonAPI,
    routeProps: any,
}

export interface SubmitJobComponentState {
    component: null | ReactElement<any>,
}

/**
 * Implements the job submission UI as an asynchronous
 * state machine.
 *
 * Each "state" is a component. A component transitions to
 * the next state by calling onTransitionRequest with the
 * next component.
 */
export class SubmitJobComponent extends React.Component<SubmitJobComponentProps, SubmitJobComponentState> {

    public constructor(props: SubmitJobComponentProps, context: any) {
        super(props, context);

        this.state = {
            component: null,
        };
    }

    public componentDidMount(): void {
        const initialComponent = (
            <LoadingJobSpecsState
                api={this.props.api}
                routeProps={this.props.routeProps}
                onTransitionRequest={this.onTransitionRequest.bind(this)}/>
        );

        this.onTransitionRequest(initialComponent);
    }

    private onTransitionRequest(component: ReactElement<any>): void {
        this.setState({component});
    }

    public render(): null | ReactElement<any> {
        return this.state.component;
    }
}
