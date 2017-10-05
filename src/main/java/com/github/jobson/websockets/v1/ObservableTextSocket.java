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

package com.github.jobson.websockets.v1;

import io.reactivex.Observable;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;

@WebSocket
public final class ObservableTextSocket extends ObservableSocket<String> {

    public ObservableTextSocket(String name, Observable<String> events) {
        super(name, events);
    }

    @Override
    protected void onMessage(String messageData) {
        if (this.session == null) return;

        try {
            this.session.getRemote().sendString(messageData);
        } catch (IOException ex) {
            log.error("Could not send stderr to " + session.getRemote().toString() +
                    ": " + ex.getMessage());
        }
    }
}
