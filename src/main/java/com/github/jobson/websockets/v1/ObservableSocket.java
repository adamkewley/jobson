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
import io.reactivex.disposables.Disposable;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@WebSocket
public abstract class ObservableSocket<T> {

    private static final int NORMAL_SOCKET_CLOSURE_STATUS = 1000;
    private static final int SERVER_UNEXPECTED_CONDITION_STATUS = 1011;


    protected final Logger log;
    private final Disposable eventsSubscription;
    protected Session session;


    public ObservableSocket(String name, Observable<T> events) {
        this.log = LoggerFactory.getLogger(
                ObservableSocket.class.getName() + "(" + name + ")");

        this.eventsSubscription = events.subscribe(
                this::onMessage,
                this::onObservableError,
                this::onObservableClosed);
    }

    protected abstract void onMessage(T messageData) throws IOException;

    private void onObservableError(Throwable ex) {
        log.debug("Closing websocket because an error was thrown by the observable. Error: " + ex);
        this.session.close(SERVER_UNEXPECTED_CONDITION_STATUS, "Internal server error");
    }

    private void onObservableClosed() {
        log.debug("Closing websocket because observable closed");
        this.session.close(NORMAL_SOCKET_CLOSURE_STATUS, "Sever event stream ended");
    }

    @OnWebSocketConnect
    public void onWebSocketConnect(Session session) {
        log.debug("Opening websocket");
        this.session = session;
    }

    @OnWebSocketClose
    public void onWebSocketClose(Session session, int closeCode, String closeReason) {
        log.debug("Closing websocket");
        this.eventsSubscription.dispose();
        session.close(closeCode, closeReason);
    }

    @OnWebSocketError
    public void onWebSocketError(Session session, Throwable ex) {
        log.debug(ex.getMessage());
        this.eventsSubscription.dispose();
    }
}
