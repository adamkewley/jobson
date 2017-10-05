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
import java.nio.ByteBuffer;

@WebSocket
public final class ObservableByteArraySocket extends ObservableSocket<byte[]> {

    public ObservableByteArraySocket(String name, Observable<byte[]> events) {
        super(name, events);
    }

    @Override
    protected void onMessage(byte[] messageData) throws IOException {
        try {
            if (this.session == null) return;
            this.session.getRemote().sendBytes(ByteBuffer.wrap(messageData));
        } catch (IOException ex) {
            log.error("Could not send stdio to " + session.getRemote().toString() +
                    ": " + ex.getMessage());
        }
    }
}
