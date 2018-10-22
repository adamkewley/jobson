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
import {Component, FormEvent, ReactElement} from "react";
import {APIJobSpecSummary} from "../apitypes/APIJobSpecSummary";

export interface JobSpecSelectorComponentProps {
    selectedSpecId: string,
    specs: APIJobSpecSummary[],
    onSelectedSpecIdChanged: (specId: string) => void,
}

export class JobSpecSelectorComponent extends Component<JobSpecSelectorComponentProps> {

    private static renderJobSpecSummary(jobSpecSummary: APIJobSpecSummary, i: number): ReactElement<any> {
        return (
            <option key={i} value={jobSpecSummary.id}>
                {jobSpecSummary.name}
            </option>
        );
    }

    public render(): ReactElement<any> {
        return (
            <div className="ui form">
                <div className="field">
                    <label htmlFor="job-spec">
                        Job Spec
                    </label>
                    <select id="job-spec"
                            className="ui fluid dropdown"
                            value={this.props.selectedSpecId}
                            onChange={this.onChangedSelectedSpec.bind(this)}>

                        {this.props.specs.map(JobSpecSelectorComponent.renderJobSpecSummary)}
                    </select>
                    {}
                </div>
                <div className="ui info message">
                    {this.selectedSpec.description}
                </div>
            </div>
        );
    }

    private onChangedSelectedSpec(e: FormEvent<HTMLSelectElement>): void {
        const newSpecId = e.currentTarget.value;
        this.props.onSelectedSpecIdChanged(newSpecId);
    }

    private get selectedSpec() {
        return this.props.specs.find(spec => spec.id === this.props.selectedSpecId);
    }
}
