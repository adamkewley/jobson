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
import {Helpers} from "../../Helpers";

interface State {
    loading: boolean;
    error: string | null;
}

interface APIFile {
    filename: string;
    data: string;  // base64 encoded
}

async function toJobsonFile(file: File): Promise<APIFile> {
    return {
        filename: file.name,
        data: await Helpers.toB64(file),
    };
}

async function toJobsonFilesArray(files: FileList): Promise<APIFile[]> {
    const ret: APIFile[] = [];
    for (let i = 0; i < files.length; ++i) {
        const file: File = files[i];
        const apiFile = await toJobsonFile(file);
        ret.push(apiFile);
    }
    return ret;
}

export class FileArrayInputEditor extends Component<InputEditorProps, State> {

    public state: State = {
        loading: false,
        error: null,
    };

    private fileInput: React.RefObject<HTMLInputElement> = React.createRef();

    public render(): ReactElement<any> {
        return (
            <div>
                {this.state.error !== null ?
                    Helpers.renderErrorMessage("Error loading file", this.state.error) :
                    <div></div>}
                <input type="file"
                       ref={this.fileInput}
                       onChange={e => this.onFileInputChanged(e)}
                       required
                       multiple
                       disabled={this.state.loading} />
            </div>
        )
    }

    private onFileInputChanged(e: React.FormEvent): void {
        const files: FileList = this.fileInput.current.files;

        this.setState({
            loading: true,
            error: null,
        }, () => {
            toJobsonFilesArray(files)
                .then(ary => {
                    const upd = InputEditorUpdate.value(ary);
                    this.props.onJobInputUpdate(upd);
                    this.setState({
                        loading: false,
                        error: null,
                    });
                })
                .catch(err => {
                    this.setState({
                        loading: false,
                        error: err.toString(),
                    });
                });
        });
    }
}
