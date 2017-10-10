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

package com.github.jobson.auth.custom;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jobson.auth.AuthenticationBootstrap;
import com.github.jobson.dao.users.UserDAO;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.Test;

import javax.servlet.Servlet;
import java.io.IOException;

import static com.github.jobson.Helpers.readJSON;
import static com.github.jobson.TestHelpers.generateClassName;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

public final class CustomAuthenticatorConfigTest {

    private static AuthenticationBootstrap createTypicalAuthBootstrap() {
        final UserDAO userDAO = mock(UserDAO.class);
        final Server s = new Server(0);
        final Servlet se = new ServletContainer();
        final JerseyEnvironment env = new JerseyEnvironment(new JerseyContainerHolder(se), new DropwizardResourceConfig());

        return new AuthenticationBootstrap(env, userDAO);
    }


    @Test(expected = NullPointerException.class)
    public void testCtorThrowsIfClassNameWasNull() {
        final CustomAuthenticatorConfig config =
                new CustomAuthenticatorConfig(null);
    }

    @Test(expected = RuntimeException.class)
    public void testCtorThrowsIfClassNameDoesNotExistOnClassPath() {
        final CustomAuthenticatorConfig config =
                new CustomAuthenticatorConfig(generateClassName());
    }

    @Test(expected = RuntimeException.class)
    public void testCtorThrowsIfClassDoesNotDeriveFromAuthenticationConfig() {
        final CustomAuthenticatorConfig config =
                new CustomAuthenticatorConfig(Object.class.getName());
    }

    @Test
    public void testCtorDoesNotThrowIfClassDoesDeriveFromAuthenticationConfig() {
        final CustomAuthenticatorConfig config =
                new CustomAuthenticatorConfig(NullCustomAuthConfig.class.getName());
    }

    @Test
    public void testEnableWithPropertiesPutsThePropetiesOnTheLoadedCustomConfig() throws IOException {
        final JsonNode n =
                readJSON("{ \"prop1\": \"val1\", \"prop2\": \"val2\" }", JsonNode.class);

        final CustomAuthenticatorConfig config =
                new CustomAuthenticatorConfig(CustomAuthConfigWithProperties.class.getName(), n);

        final CustomAuthConfigWithProperties createdConfig =
                (CustomAuthConfigWithProperties)config.getLoadedConfig();

        assertThat(createdConfig.getProp1()).isEqualTo("val1");
        assertThat(createdConfig.getProp2()).isEqualTo("val2");
    }
}