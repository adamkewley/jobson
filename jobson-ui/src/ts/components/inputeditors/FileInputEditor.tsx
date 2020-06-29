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

import {Component, ReactElement} from "react";
import {InputEditorProps} from "./InputEditor";
import * as React from "react";
import {InputEditorUpdate} from "./updates/InputEditorUpdate";

const enum States {
    Showing,
    Loading,
}

interface ShowingState {
    type: States.Showing;
    error: string | null;
}

interface LoadingState {
    type: States.Loading;
}

type State = ShowingState | LoadingState;

function toB64(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = e => {
            const contents = e.target.result as ArrayBuffer;
            const b64 = btoa(String.fromCharCode.apply(null, new Uint8Array(contents)));
            resolve(b64);
        };
        reader.onerror = e => {
            reject(e);
        };
        reader.onabort = e => {
            reject(e);
        };
        reader.readAsArrayBuffer(file);
    });
}

export class FileInputEditor extends Component<InputEditorProps, State> {

    public state: State = {
        type: States.Showing,
        error: null,
    };

    private fileInput: React.RefObject<HTMLInputElement> = React.createRef();

    public render(): ReactElement<any> {
        switch (this.state.type) {
            case States.Showing:
                return this.renderShowingState();
            case States.Loading:
                return this.renderLoadingState();
        }
    }

    private renderShowingState(): ReactElement<any> {
        return (
            <div>
                <input type="file"
                       ref={this.fileInput}
                       onChange={e => this.onFileInputChanged(e)}
                       required />
            </div>
        );
    }

    private renderLoadingState(): ReactElement<any> {
        return (
            <div>
                <input type="file"
                       ref={this.fileInput}
                       onChange={e => this.onFileInputChanged(e)}
                       required />
                Loading...
            </div>
        );
    }

    private onFileInputChanged(e: React.FormEvent): void {
        const file: File = this.fileInput.current.files[0];
        const filename = file.name;

        this.setState({
            type: States.Loading,
        }, () => {
            toB64(file)
                .then(b64Str => {
                    const upd = InputEditorUpdate.value({
                        filename: file.name,
                        data: b64Str,
                    });
                    this.props.onJobInputUpdate(upd);
                    this.setState({
                        type: States.Showing,
                        error: null,
                    });
                })
                .catch(err => {
                    this.setState({
                        type: States.Showing,
                        error: err,
                    });
                })
        });
    }
}
