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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Either<T, U> {

    public static <T, U> Either<T, U> left(T val) {
        return new Either<>(val, null);
    }

    public static <T, U> Either<T, U> right(U val) {
        return new Either<>(null, val);
    }


    private T l;
    private U r;


    private Either(T l, U r) {
        this.l = l;
        this.r = r;
    }


    public <V> V visit(EitherVisitorT<T, U, V> visitor) {
        if (l != null) return visitor.whenLeft(l);
        else return visitor.whenRight(r);
    }

    public void visit(EitherVisitor<T, U> visitor) {
        if (l != null) visitor.whenLeft(l);
        else visitor.whenRight(r);
    }

    public <V> Either<V, U> leftFlatMap(Function<T, Either<V, U>> f) {
        if (l != null) return f.apply(l);
        else return Either.right(r);
    }

    public <V> Either<V, U> leftMap(Function<T, V> f) {
        if (l != null) return Either.left(f.apply(l));
        else return Either.right(r);
    }

    public <V, W> Either<V, W> biMap(Function<T, V> lf, Function<U, W> rf) {
        if (l != null) return Either.left(lf.apply(l));
        else return Either.right(rf.apply(r));
    }

    public void ifLhsHasValue(Consumer<T> f) {
        if (l != null) f.accept(l);
    }

    public void ifRhsHasValue(Consumer<U> f) {
        if (r != null) f.accept(r);
    }

    public void handleBoth(Consumer<T> fl, Consumer<U> fr) {
        if (l != null) fl.accept(l);
        else fr.accept(r);
    }

    public Optional<T> getLeft() {
        if (l != null) return Optional.of(l);
        else return Optional.empty();
    }

    public Optional<U> getRight() {
        if (r != null) return Optional.of(r);
        else return Optional.empty();
    }
}
