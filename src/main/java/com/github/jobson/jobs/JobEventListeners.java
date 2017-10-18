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

package com.github.jobson.jobs;

import io.reactivex.Observer;
import io.reactivex.subjects.PublishSubject;

public interface JobEventListeners {

    static JobEventListeners createNullListeners() {
        return create(PublishSubject.create(), PublishSubject.create());
    }

    static JobEventListeners createStderrListener(Observer<byte[]> stderrObserver) {
        return JobEventListeners.create(PublishSubject.create(), stderrObserver);
    }

    static JobEventListeners createStdoutListener(Observer<byte[]> stdoutObserver) {
        return JobEventListeners.create(stdoutObserver, PublishSubject.create());
    }

    static JobEventListeners create(Observer<byte[]> stdoutObserver, Observer<byte[]> stderrObserver) {
        return new JobEventListeners() {
            @Override
            public Observer<byte[]> getOnStdoutListener() {
                return stdoutObserver;
            }

            @Override
            public Observer<byte[]> getOnStderrListener() {
                return stderrObserver;
            }
        };
    }

    Observer<byte[]> getOnStdoutListener();
    Observer<byte[]> getOnStderrListener();
}
