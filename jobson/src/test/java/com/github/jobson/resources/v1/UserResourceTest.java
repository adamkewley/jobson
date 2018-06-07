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

package com.github.jobson.resources.v1;

import com.github.jobson.TestHelpers;
import com.github.jobson.api.v1.APIUserDetails;
import com.github.jobson.api.v1.UserId;
import org.junit.Test;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;

public final class UserResourceTest {

    @Test
    public void testGetCurrentUserReturnsCurrentUserId() {
        final UserId userId = TestHelpers.generateUserId();

        final SecurityContext securityContext = new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return new Principal() {
                    @Override
                    public String getName() {
                        return userId.toString();
                    }
                };
            }

            @Override
            public boolean isUserInRole(String s) {
                return false;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public String getAuthenticationScheme() {
                return null;
            }
        };

        final UserResource userResource = new UserResource();

        final APIUserDetails APIUserDetails = userResource.fetchCurrentUserDetails(securityContext);

        assertThat(APIUserDetails.getId()).isEqualTo(userId);
    }
}