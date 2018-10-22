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

import {Helpers} from "./Helpers";
import {map} from "rxjs/operators";
import {HttpService} from "./HttpService";
import {APIJobSpecSummary} from "./components/apitypes/APIJobSpecSummary";
import {APIJobSpec} from "./components/apitypes/APIJobSpec";
import {APIJobDetails} from "./components/apitypes/APIJobDetails";
import {APIJobDetailsCollection} from "./components/apitypes/APIJobDetailsCollection";
import {APIJobRequest} from "./components/apitypes/APIJobRequest";
import {APIJobCreatedResponse} from "./components/apitypes/APIJobCreatedResponse";
import {Observable} from "rxjs/index";
import {APIJobEvent} from "./components/apitypes/APIJobEvent";
import {APIJobOutput} from "./components/apitypes/APIJobOutput";

export class JobsonAPI {

    private readonly http: HttpService;
    private readonly apiPrefix: string;


    constructor(httpService: HttpService, apiPrefix: string) {
        this.http = httpService;
        this.apiPrefix = apiPrefix;
    }


    urlToJobSpecSummaries(): string {
        return `${this.apiPrefix}/v1/specs`;
    }

    buildAPIPathTo(subPath: string): string {
        return `${this.apiPrefix}` + subPath;
    }

    fetchJobSpecSummaries(): Promise<APIJobSpecSummary[]> {
        return this.http
            .get(this.urlToJobSpecSummaries())
            .then(resp => JSON.parse(resp).entries);
    }


    urlToJobSpec(id: string): string {
        return `${this.apiPrefix}/v1/specs/${id}`;
    }

    fetchJobSpec(id: string): Promise<APIJobSpec> {
        return this.http
            .get(this.urlToJobSpec(id))
            .then(resp => JSON.parse(resp));
    }


    urlToJobSummaries(query: string = "", page: number = 0): string {
        return query.length > 0 ?
            `${this.apiPrefix}/v1/jobs?query=${query}&page=${page}` :
            `${this.apiPrefix}/v1/jobs?page=${page}`;
    }

    fetchJobSummaries(query: string = "", page: number = 0): Promise<APIJobDetailsCollection> {
        return this.http
            .get(this.urlToJobSummaries(query, page))
            .then(resp => JSON.parse(resp));
    }


    urlToSubmitJobRequest(): string {
        return `${this.apiPrefix}/v1/jobs`;
    }

    submitJobRequest(jobRequest: APIJobRequest): Promise<APIJobCreatedResponse> {
        return this.http
            .post(this.urlToSubmitJobRequest(), jobRequest)
            .then(resp => JSON.parse(resp));
    }


    urlToGetJobDetailsById(jobId: string): string {
        return `${this.apiPrefix}/v1/jobs/${jobId}`;
    }

    fetchJobDetailsById(jobId: string): Promise<APIJobDetails> {
        return this.http
            .get(this.urlToGetJobDetailsById(jobId))
            .then(resp => JSON.parse(resp));
    }


    urlToGetJobStderr(jobId: string): string {
        return `${this.apiPrefix}/v1/jobs/${jobId}/stderr`;
    }

    fetchJobStderr(jobId: string): Promise<Blob> {
        return this.http.getRaw(this.urlToGetJobStderr(jobId));
    }


    urlToGetJobStdout(jobId: string): string {
        return `${this.apiPrefix}/v1/jobs/${jobId}/stdout`;
    }

    fetchJobStdout(jobId: string): Promise<Blob> {
        return this.http.getRaw(this.urlToGetJobStdout(jobId));
    }


    postEmptyRequestToRESTHref(href: string): Promise<string> {
        return this.http.post(`${this.apiPrefix}${href}`, {});
    }


    urlToGetJobOutputs(jobId: string): string {
        return `${this.apiPrefix}/v1/jobs/${jobId}/outputs`;
    }

    fetchJobOutputs(jobId: string): Promise<APIJobOutput[]> {
        return this.http
            .get(this.urlToGetJobOutputs(jobId))
            .then(resp => JSON.parse(resp).entries);
    }


    urlToGetJobInputs(jobId: string): string {
        return `${this.apiPrefix}/v1/jobs/${jobId}/inputs`;
    }

    fetchJobInputs(jobId: string): Promise<{ [inputId: string]: any }> {
        return this.http
            .get(this.urlToGetJobInputs(jobId))
            .then(resp => JSON.parse(resp));
    }


    urlToCurrentUser(): string {
        return `${this.apiPrefix}/v1/users/current`;
    }

    fetchCurrentUser(): Promise<string> {
        return this.http
            .get(this.urlToCurrentUser())
            .then(resp => JSON.parse(resp).id);
    }


    urlToExistingJobsSpec(jobId: string): string {
        return `${this.apiPrefix}/v1/jobs/${jobId}/spec`;
    }

    fetchExistingJobsSpec(jobId: string): Promise<APIJobSpec> {
        return this.http
            .get(this.urlToExistingJobsSpec(jobId))
            .then(resp => JSON.parse(resp));
    }


    restHrefToUrl(restHref: string): string {
        return `${this.apiPrefix}${restHref}`;
    }

    fetchJsonResource(url: string): Promise<any> {
        return this.http.get(url).then(JSON.parse);
    }


    // WebSockets:


    onJobStderrUpdate(jobId: string): Observable<Blob> {
        const url = Helpers.makeWebsocketPath(`${this.apiPrefix}/v1/jobs/${jobId}/stderr/updates`);
        return Helpers.createObservableSocket(url).pipe(map(e => e.data));
    }

    onJobStdoutUpdate(jobId: string): Observable<Blob> {
        const url = Helpers.makeWebsocketPath(`${this.apiPrefix}/v1/jobs/${jobId}/stdout/updates`);
        return Helpers.createObservableSocket(url).pipe(map(e => e.data));
    }

    onAllJobStatusChanges(): Observable<APIJobEvent> {
        const url = Helpers.makeWebsocketPath(`${this.apiPrefix}/v1/jobs/events`);
        return Helpers.createObservableSocket(url).pipe(map(e => JSON.parse(e.data)));
    }
}
