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

package com.github.jobson.scripting.functions;

import com.github.jobson.TestHelpers;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class ToStringFunctionTest {

    @Test(expected = RuntimeException.class)
    public void testCallingFunctionWithMoreThanOneArgThrowsException() throws Exception {
        final ToStringFunction f = new ToStringFunction();
        f.call(new Object(), new Object());
    }

    @Test
    public void testCallingFunctionWithOneArgResultsInToStringOfThatArg() {
        final ToStringFunction f = new ToStringFunction();
        final Integer input = TestHelpers.randomIntBetween(50, 100000);

        final Object output = f.call(input);

        assertThat(output).isInstanceOf(String.class);
        assertThat(output).isEqualTo(input.toString());
    }
}