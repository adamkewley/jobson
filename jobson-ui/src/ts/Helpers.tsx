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
import {Subject, fromEvent, merge, Observer, Observable} from "rxjs";
import {delay, take} from "rxjs/operators";
import {Link} from "react-router-dom";
import {JobsonAPI} from "./JobsonAPI";
import {APIRestLink} from "./components/apitypes/APIRestLink";
import {APIErrorMessage} from "./components/apitypes/APIErrorMessage";

export class Helpers {

    public static promptUserToDownloadAsJSON(obj: any): void {
        const objJSON = JSON.stringify(obj, null, 2);
        const blob = new Blob([objJSON], {type: "application/json"});
        Helpers.promptUserToDownload(blob, "query.json");
    }

    public static promptUserToDownload(blob: Blob, fileName: string): void {
        // If it's shitty IE
        if (window.navigator.msSaveOrOpenBlob) {
            window.navigator.msSaveOrOpenBlob(blob, fileName);
        } else {
            const blobUrl = URL.createObjectURL(blob);

            const downloadLink = document.createElement("a");
            downloadLink.href = blobUrl;
            downloadLink.download = fileName;
            downloadLink.style.visibility = "hidden";

            document.body.appendChild(downloadLink);
            downloadLink.click();
            document.body.removeChild(downloadLink);
        }
    }

    public static extractParams(search: string): { [k: string]: any } {
        if (!search) return {};

        let pairs = search.substring(1).split("&"),
            obj: { [k: string]: string } = {},
            pair,
            i;

        for (i in pairs) {
            if (pairs[i] === "") continue;

            pair = pairs[i].split("=");
            obj[decodeURIComponent(pair[0])] = decodeURIComponent(pair[1]);
        }

        return obj;
    }

    public static promptUserForFiles(mimeTypes: string = "", allowMultipleFiles: boolean = true): Promise<FileList> {
        return new Promise((resolve, reject) => {
            const fileInputEl = document.createElement("input");

            if (allowMultipleFiles)
                fileInputEl.multiple = true;

            fileInputEl.style.visibility = "hidden";
            fileInputEl.type = "file";
            fileInputEl.accept = mimeTypes;

            const changeEvents = fromEvent(fileInputEl, "change");
            const focusEvents = fromEvent(document, "focus").pipe(delay(50));

            merge(changeEvents, focusEvents)
                .pipe(take(1))
                .subscribe(() => {
                    if (fileInputEl.files !== null && fileInputEl.files.length === 1)
                        resolve(fileInputEl.files);
                    else reject("User did not select a file");
                });

            document.body.appendChild(fileInputEl);
            fileInputEl.click();
            document.body.removeChild(fileInputEl);
        });
    }

    public static createObservableSocket(wsURL: string): Observable<any> {
        const subject = new Subject();
        Helpers._observeWebsocket(wsURL, subject);
        return subject;
    }

    private static _observeWebsocket(wsURL: string, observer: Observer<any>): void {
        const ws = new WebSocket(wsURL);

        ws.onmessage = (e) => observer.next(e);
        ws.onerror = (e) => observer.error(e);

        ws.onclose = (e) => {
            if (Helpers._websocketClosedDueToTimeout(e)) {
                this._observeWebsocket(wsURL, observer);
            } else {
                observer.complete();
            }
        };
    }

    private static _websocketClosedDueToTimeout(wsCloseEvent: CloseEvent): boolean {
        const WEBSOCKET_IDLE_TIMEOUT_CODE = 1001;
        return wsCloseEvent.code === WEBSOCKET_IDLE_TIMEOUT_CODE;
    }

    public static promptUserForFile(mimeTypes = ""): Promise<File> {
        return Helpers.promptUserForFiles(mimeTypes, false).then(files => files[0]);
    }

    public static readFileAsText(file: Blob): Promise<string> {
        return new Promise((resolve, reject) => {
            const textReader = new FileReader();
            textReader.onload = () => resolve(textReader.result as string);
            textReader.onerror = () => reject(textReader.error);
            textReader.readAsText(file);
        });
    }

    static dissasoc(o: any, k: string): any {
        const copy = Object.assign({}, o);
        delete copy[k];
        return copy;
    }

    static fetchBlobContentsAsText(blob: Blob): Promise<string> {
        return new Promise((resolve) => {
            const reader = new FileReader();
            reader.onload = () => {
                resolve(reader.result as string);
            };
            reader.readAsText(blob);
        });
    }

    static reject<T>(ary: T[], el: T): T[] {
        const i = ary.indexOf(el);
        return ary.filter((e, j) => j !== i);
    }

    static makeWebsocketPath(p: string): string {
        const scheme = window.location.protocol.startsWith("https") ? "wss" : "ws";
        const prefix = `${scheme}://${window.location.host}`;

        if (p.startsWith("/")) {
            return prefix + p;
        } else if (window.location.pathname.endsWith("/")) {
            return prefix + window.location.pathname + p;
        } else {
            return prefix + window.location.pathname + "/" + p;
        }
    }

    static isEmpty(o: any): boolean {
        return Object.keys(o).length === 0;
    }

    static deepEquals(o1: any, o2: any): boolean {
        return o1 === o2; // TODO: Recursive impl.
    }

    static forEachKv<V>(f: (k: string, v: V) => void, o: { [key: string]: V }): void {
        Object.keys(o).forEach(k => f(k, o[k]));
    }

    static mapKv<V1, V2>(f: (k: string, v: V1) => V2, o: { [key: string]: V1 }): { [key: string]: V2 } {
        const ret: { [k: string]: V2 } = {};
        Object.keys(o).forEach(k => ret[k] = f(k, o[k]));
        return ret;
    }

    public static mapKvToArray<V1, V2>(f: (k: string, v: V1) => V2, o: { [key: string]: V1 }): V2[] {
        return Object.keys(o).map(k => f(k, o[k]));
    }

    static renderStatusField(status: string): ReactElement<any> {
        switch (status) {
            case "aborted":
                return <div className="ui orange horizontal basic label">{status}</div>;
            case "fatal-error":
                return <div className="ui red horizontal basic label">{status}</div>;
            case "finished":
                return <div className="ui green horizontal basic label">{status}</div>;
            case "running":
                return (
                    <div className="ui horizontal basic label">
                        <div className="ui tiny active inline loader"/>
                        Running
                    </div>
                );
            default:
                return <div className="ui horizontal basic label">{status}</div>;
        }
    }

    static renderDownloadButton(href: string): ReactElement<any> {
        return (
            <a className="ui right floated primary button"
               href={href}>
                <i className="download icon"/>
                Download
            </a>
        );
    }

    static jobStatusColor(jobStatus: string): string {
        switch (jobStatus) {
            case "submitted":
                return "grey";
            case "fatal-error":
                return "red";
            case "finished":
                return "green";
            case "running":
                return "grey";
            case "aborted":
                return "orange";
            default:
                return "grey";
        }
    }

    static renderLoadingMessage(noun: string): ReactElement<any> {
        return (
            <div className="ui icon message">
                <i className="notched circle loading icon"/>
                <div className="content">
                    <div className="header">
                        Loading
                    </div>
                    <p>
                        Fetching {noun} from the Jobson API.
                    </p>
                </div>
            </div>
        );
    }

    static renderAPIErrorMessage(noun: string, apiError: APIErrorMessage, retryCallback: (() => void)): ReactElement<any> {
        const header = <span>Error loading {noun}</span>;
        const body =
            <div>
                <p>
                    There was an error loading {noun} from the Jobson API.
                    The API's error message was: {apiError.message}.
                </p>
                <button className="ui primary icon button"
                        onClick={retryCallback}>
                    <i className="refresh icon"/>
                    Try Again
                </button>
            </div>;

        return Helpers.renderErrorMessage(header, body);
    }

    static renderErrorMessage(header: ReactElement<any> | string, content: ReactElement<any> | string): ReactElement<any> {
        return (
            <div className="ui negative icon message">
                <i className="warning circle icon"/>
                <div className="content">
                    <div className="header">
                        {header}
                    </div>
                    {content}
                </div>
            </div>
        );
    }

    static renderWarningMessage(header: ReactElement<any> | string, content: ReactElement<any> | string): ReactElement<any> {
        return (
            <div className="ui yellow icon message">
                <i className="warning circle icon"/>
                <div className="content">
                    <div className="header">
                        {header}
                    </div>
                    {content}
                </div>
            </div>
        );
    }

    static renderAllJobActions(jobsonApi: JobsonAPI, jobId: string, restLinks: { [name: string]: APIRestLink }): ReactElement<any>[] {
        const btnsWithoutView =
            Helpers.renderJobActionsWithoutViewBtn(jobsonApi, jobId, restLinks);

        const selfLinkIdx = Object.keys(restLinks).indexOf("self");

        if (selfLinkIdx !== -1) {
            const viewBtn = (
                <Link to={"/jobs/" + jobId}
                      className="ui tiny compact button"
                      key={selfLinkIdx}>
                    View
                </Link>
            );
            return btnsWithoutView.concat([viewBtn]);
        } else {
            return btnsWithoutView;
        }
    }

    static renderJobActionsWithoutViewBtn(jobsonApi: JobsonAPI, jobId: string, restLinks: { [name: string]: APIRestLink }): any[] {
        const restLinkActions: (ReactElement<any> | null)[] = Object.keys(restLinks)
            .map((linkName, i) => {
                switch (linkName) {
                    case "abort":
                        const href = restLinks[linkName].href;
                        return (
                            <button className="ui tiny compact negative button"
                                    key={i}
                                    onClick={() => jobsonApi.postEmptyRequestToRESTHref(href)}>
                                Abort
                            </button>
                        );
                    default:
                        return null;
                }
            })
            .filter(el => el !== null);

        const copyLink = <Link to={"/submit?based-on=" + jobId}
                               className="ui tiny compact button"
                               key={Object.keys(restLinks).length}>
            Copy
        </Link>;

        return restLinkActions.concat([copyLink]);
    }
}
