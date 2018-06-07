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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public final class SimpleCancelablePromise<T> implements CancelablePromise<T> {

    private CompletableFuture<T> internalPromise = new CompletableFuture<>();
    private List<Runnable> onCancelCallbacks = new ArrayList<>();
    private boolean isCancelled = false;


    public boolean completeExceptionally(Throwable ex) {
        return internalPromise.completeExceptionally(ex);
    }

    public boolean complete(T value) {
        return internalPromise.complete(value);
    }

    public void onCancel(Runnable f) {
        if (!isCancelled)
            onCancelCallbacks.add(f);
    }

    public boolean cancel(boolean mayInterrupt) {
        if (!isCancelled) {
            isCancelled = true;
            onCancelCallbacks.forEach(Runnable::run);
            return true;
        } else return false;
    }

    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return internalPromise.thenAccept(action);
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return internalPromise.get(timeout, unit);
    }

    @Override
    public T get() throws InterruptedException, ExecutionException, TimeoutException {
        return internalPromise.get();
    }
}
