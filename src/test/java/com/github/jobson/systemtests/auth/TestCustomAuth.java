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

import com.github.jobson.config.ApplicationConfig;
import com.github.jobson.resources.v1.UserResource;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import static com.github.jobson.Constants.HTTP_USERS_PATH;
import static com.github.jobson.HttpStatusCodes.OK;
import static com.github.jobson.HttpStatusCodes.UNAUTHORIZED;
import static com.github.jobson.TestHelpers.generateRandomString;
import static com.github.jobson.systemtests.SystemTestHelpers.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public final class TestCustomAuth {

    private static final String USERNAME_IN_CONFIG_TEMPLATE = "some-username";
    private static final String PASSWORD_IN_CONFIG_TEMPLATE = "some-password";

    @ClassRule
    public static final DropwizardAppRule<ApplicationConfig> RULE =
            createStandardRuleWithTemplate("fixtures/systemtests/auth/custom-auth-config.yml");


    @Test
    public void testBoots() {}

    @Test
    public void testAcceptsCredentialsHardCodedByTheCustomAuthScheme() {
        final Invocation.Builder b = generateRequest(RULE, HTTP_USERS_PATH + "/current");
        authenticate(b, USERNAME_IN_CONFIG_TEMPLATE, PASSWORD_IN_CONFIG_TEMPLATE);

        final Response response = b.get();

        assertThat(response.getStatus()).isEqualTo(OK);
    }

    @Test
    public void testRejectsRequestsIfTheyDoNotMatchCustomScheme() {
        final Invocation.Builder b = generateRequest(RULE, HTTP_USERS_PATH + "/current");
        authenticate(b, USERNAME_IN_CONFIG_TEMPLATE, generateRandomString());

        final Response response = b.get();

        assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED);
    }
}
