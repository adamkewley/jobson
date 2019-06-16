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

import {Component, ReactElement} from "react";
import {JobsonAPI} from "../JobsonAPI";
import {APIErrorMessage} from "../apitypes/APIErrorMessage";
import {APIJobRequest} from "../apitypes/APIJobRequest";


export interface ResubmitButtonProps {
    api: JobsonAPI;
    jobId: string;
    routeProps: any;
};

export interface ResubmitButtonState {
    isSubmitting: boolean;
    errorSubmitting: APIErrorMessage | null,
};

export class ResubmitButtonComponent extends Component<ResubmitButtonProps, ResubmitButtonState> {

    public constructor(props: ResubmitButtonProps, context: any) {
        super(props, context);

        this.state = {
            isSubmitting: false,
            errorSubmitting: null,
        };
    }

    public componentWillReceiveProps(newProps: ResubmitButtonProps): void {
        this.setState({
            isSubmitting: false,
            errorSubmitting: null,
        });
    }    

    public render(): ReactElement<any> {
        const state = this.state;
        if (state.errorSubmitting !== null) {
            return this.renderError();
        } else if (state.isSubmitting) {
            return this.renderLoading();
        } else {
            return this.renderButton();
        }
    }

    public renderButton(): ReactElement<any> {
        return (
            <button className="ui tiny compact button" onClick={this.onClick.bind(this)}>
                Resubmit
            </button>
        );
    }

    public renderError(): ReactElement<any> {
        const err = this.state.errorSubmitting;
        return (
                <button className="ui tiny compact disabled red button">
                Error: {err.code}: {err.message}
                </button>
        );
    }

    public renderLoading(): ReactElement<any> {
        return (
            <button className="ui tiny compact disabled button">
                Resubmitting...
            </button>
        );
    }

    private onClick(): void {
        this.setState({ isSubmitting: true }, () => {
            const api = this.props.api;
            const jobId = this.props.jobId;
            const jobDetailsPromise =  api.fetchJobDetailsById(jobId);
            const jobInputsPromise = api.fetchJobInputs(jobId);
            const jobSpecPromise = api.fetchExistingJobsSpec(jobId);

            Promise.all([jobDetailsPromise, jobInputsPromise, jobSpecPromise])
                .then((values) => {
                    const jobReq: APIJobRequest = {
                        spec: values[2].id,
                        name: values[0].name,
                        inputs: values[1],
                    };
                    return api.submitJobRequest(jobReq);
                })
                .then(resp => {
                    this.props.routeProps.history.push(`/jobs/${resp.id}`);
                    window.location.reload();
                })
                .catch(e => this.setState({ errorSubmitting: e }));
        });
    }
    
}
