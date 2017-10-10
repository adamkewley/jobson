/*
 * Gaia CU5 SUEDE
 *
 * (c) 2005-2020 Gaia Data Processing and Analysis Consortium
 *
 *
 * CU5 SUEDE software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * CU5 SUEDE software is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this CU5 software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 *-----------------------------------------------------------------------------
 */

package com.github.jobson.systemtests.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.jobson.auth.AuthenticationBootstrap;
import com.github.jobson.auth.PermitAllAuthorizer;
import com.github.jobson.config.AuthenticationConfig;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;

import javax.validation.constraints.NotNull;
import java.security.Principal;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, visible = true)
public final class SystemtestCustomAuthConfig implements AuthenticationConfig {

    @JsonProperty
    @NotNull
    private String username;

    @JsonProperty
    @NotNull
    private String password;

    @Override
    public AuthFilter<?, Principal> createAuthFilter(AuthenticationBootstrap bootstrap) {
        return new BasicCredentialAuthFilter.Builder<>()
                .setAuthenticator(new SpecificUsernamePwAuthenticator(username, password))
                .setAuthorizer(new PermitAllAuthorizer())
                .buildAuthFilter();
    }
}
