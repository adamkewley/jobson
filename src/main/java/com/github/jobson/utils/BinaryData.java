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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static com.github.jobson.Constants.DEFAULT_BINARY_MIME_TYPE;

public final class BinaryData {

    public static BinaryData wrap(byte[] bytes) {
        return new BinaryData(new ByteArrayInputStream(bytes), bytes.length);
    }

    public static BinaryData wrap(byte[] bytes, String mimeType) {
        return new BinaryData(new ByteArrayInputStream(bytes), bytes.length, mimeType);
    }


    private final InputStream data;
    private final long sizeOf;
    private final String mimeType;

    public BinaryData(InputStream data, long sizeOf) {
        this.data = data;
        this.sizeOf = sizeOf;
        this.mimeType = DEFAULT_BINARY_MIME_TYPE;
    }

    public BinaryData(InputStream data, long sizeOf, String mimeType) {
        this.data = data;
        this.sizeOf = sizeOf;
        this.mimeType = mimeType;
    }

    public InputStream getData() {
        return data;
    }

    public long getSizeOf() {
        return sizeOf;
    }

    public String getMimeType() {
        return mimeType;
    }


    public BinaryData withMimeType(String mimeType) {
        return new BinaryData(data, sizeOf, mimeType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BinaryData that = (BinaryData) o;

        if (sizeOf != that.sizeOf) return false;
        return mimeType != null ? mimeType.equals(that.mimeType) : that.mimeType == null;
    }

    @Override
    public int hashCode() {
        int result = data != null ? data.hashCode() : 0;
        result = 31 * result + (int) (sizeOf ^ (sizeOf >>> 32));
        result = 31 * result + (mimeType != null ? mimeType.hashCode() : 0);
        return result;
    }
}
