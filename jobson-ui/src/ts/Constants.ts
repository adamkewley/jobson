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

export class Constants {
    public static readonly I32_MIN = -2147483647;  // (2^31)-1 (signed)
    public static readonly I32_MAX = 2147483647;  // (2^31)-1 (signed)
    public static readonly I64_MIN = -9223372036854775807; // (2^63)-1 (signed)
    public static readonly I64_MAX = 9223372036854775807;  // (2^63)-1 (signed)
    public static readonly F32_MIN = -3.402823e38;  // IEEE 754 approx. single precision min
    public static readonly F32_MAX = 3.402823e38;   // IEEE 754 approx. single precision max
    public static readonly F64_MIN = -Number.MIN_VALUE;  // This is actually higher than IEEE 754 doubles, but it's a hard javascript limit
    public static readonly F64_MAX = Number.MAX_VALUE;  // This is actually lower than IEEE 754 doubles, but it's a hard javascript limit

    public static readonly STR_ARRAY_INTERACTIVE_BREAKPOINT = 500;
    public static readonly DEFAULT_JOB_NAME = "default";
}
