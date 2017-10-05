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

package com.github.jobson.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public interface CancelablePromise<T> {

    boolean completeExceptionally(Throwable ex);
    boolean complete(T value);
    void onCancel(Runnable f);
    boolean cancel(boolean mayInterrupt);
    CompletableFuture<Void> thenAccept(Consumer<? super T> action);
    T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
    T get() throws InterruptedException, ExecutionException, TimeoutException;
}
