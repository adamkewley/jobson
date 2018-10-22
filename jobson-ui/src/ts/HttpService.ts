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

import {BehaviorSubject} from "rxjs";
import {Helpers} from "./Helpers";

export class HttpService {

    static tryParseErrorResponse(xmlHttpReq: XMLHttpRequest) {
        try {
            return JSON.parse(xmlHttpReq.response);
        } catch (e) {
            return {
                code: xmlHttpReq.status,
                message: "Error",
            };
        }
    }


    public readonly onRequestsChanged: BehaviorSubject<XMLHttpRequest[]>;


    constructor() {
        this.onRequestsChanged = new BehaviorSubject([]);
    }


    dispatch(req: XMLHttpRequest, body: string | null = null): Promise<any> {
        return new Promise((resolve, reject) => {

            this.onRequestsChanged.next(
                this.onRequestsChanged.getValue().concat([req]));

            req.onload = () => {
                this.updateSubjectWithCompletedReq(req);

                if (200 <= req.status && req.status < 300) {
                    resolve(req.response);
                } else {
                    reject(HttpService.tryParseErrorResponse(req));
                }
            };

            req.onerror = () => {
                this.updateSubjectWithCompletedReq(req);

                const response = req.status === 0 ?
                    {message: "Connection error", status: 0} :
                    HttpService.tryParseErrorResponse(req);

                reject(response);
            };

            if (body !== null) req.send(body);
            else req.send();
        });
    }

    updateSubjectWithCompletedReq(xmlHttpReq: XMLHttpRequest) {
        const oldReqs = this.onRequestsChanged.getValue();
        const newReqs = Helpers.reject(oldReqs, xmlHttpReq);
        this.onRequestsChanged.next(newReqs);
    }

    get(href: string): Promise<string> {
        const req = new XMLHttpRequest();
        req.open("GET", href);
        req.setRequestHeader("Accept", "application/json");

        return this.dispatch(req);
    }

    getRaw(href: string): Promise<Blob> {
        const req = new XMLHttpRequest();
        req.open("GET", href);
        req.responseType = "blob";

        return this.dispatch(req);
    }

    post(href: string, body: object): Promise<string> {
        const req = new XMLHttpRequest();
        req.open("POST", href);
        req.setRequestHeader("Content-Type", "application/json");
        req.setRequestHeader("Accept", "application/json");
        req.overrideMimeType("text/plain");

        return this.dispatch(req, JSON.stringify(body));
    }
}
