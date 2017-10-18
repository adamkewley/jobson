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

package com.github.jobson.auth.guest;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.PrincipalImpl;

import java.security.Principal;
import java.util.Optional;

/**
 * An authenticator that always authenticates, returning a principal with the
 * ctor-supplied username.
 */
public final class GuestAuthenticator implements Authenticator<Void, Principal> {

    private final Optional<Principal> p;

    public GuestAuthenticator(String guestUserName) {
        this.p = Optional.of(new PrincipalImpl(guestUserName));
    }

    @Override
    public Optional<Principal> authenticate(Void aVoid) throws AuthenticationException {
        return p;
    }
}
