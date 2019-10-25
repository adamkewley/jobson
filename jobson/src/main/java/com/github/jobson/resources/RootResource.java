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

package com.github.jobson.resources;

import com.github.jobson.Constants;
import com.github.jobson.api.APIRootResponse;
import com.github.jobson.api.v1.APIRestLink;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static com.github.jobson.Constants.HTTP_ROOT;

@OpenAPIDefinition(
    info = @Info(description = "Top-level resource, which lists sub-resources"))
@Path(HTTP_ROOT)
@Produces("application/json")
public final class RootResource {

    @GET
    @PermitAll
    public APIRootResponse get(@Context SecurityContext context) {
        final Map<String, APIRestLink> links = new HashMap<>();
        links.put("v1", new APIRestLink(URI.create(Constants.HTTP_V1_ROOT)));
        return new APIRootResponse(links);
    }
}
