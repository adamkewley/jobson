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

package com.github.jobson.systemtests;

import com.github.jobson.TestConstants;
import com.github.jobson.config.ApplicationConfig;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.client.Invocation;

import static com.github.jobson.Constants.HTTP_USERS_PATH;
import static com.github.jobson.systemtests.SystemTestHelpers.createStandardRule;
import static com.github.jobson.systemtests.SystemTestHelpers.generateAuthenticatedRequest;

@Ignore
public final class TestBurnIn {

    @ClassRule
    public static final DropwizardAppRule<ApplicationConfig> RULE = createStandardRule();

    @Test
    public void testGetCurrentUserCanBeCalledManyTimesWithoutCrashing() {
        final Invocation.Builder req =
                generateAuthenticatedRequest(RULE, HTTP_USERS_PATH + "/current");

        for (int i = 0; i < TestConstants.NUMBER_OF_BURN_IN_API_CALLS; i++) {
            req.get();
        }
    }
}
