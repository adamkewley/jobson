package com.github.jobson.auth.jwt;

import com.github.jobson.TestHelpers;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.auth.PrincipalImpl;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;
import java.util.*;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class JsonWebTokenAuthFilterTest {

    private static final String EXAMPLE_JWT_STRING = "0233994890.sdfksflks.sdfiosduio";

    @Test
    public void testConstructorDoesNotThrowWithValidArguments() throws AuthenticationException {
        // Shouldn't throw
        createValidAuthFilterInstance();
    }

    private static JsonWebTokenAuthFilter<Principal> createValidAuthFilterInstance() throws AuthenticationException {
        final Authenticator<String, Principal> authenticator = generateAlwaysAuthenticatesMock();
        final Authorizer<Principal> authorizer = generateAlwaysAuthorizedMock();

        return createAuthFilter(authenticator, authorizer);
    }

    private static JsonWebTokenAuthFilter<Principal> createAuthFilter(Authenticator<String, Principal> authenticator, Authorizer<Principal> authorizer) {
        return (JsonWebTokenAuthFilter<Principal>)new JsonWebTokenAuthFilter.Builder().setAuthenticator(authenticator).setAuthorizer(authorizer).buildAuthFilter();
    }

    private static Authenticator<String, Principal> generateAlwaysAuthenticatesMock() throws AuthenticationException {
        final Authenticator<String, Principal> authenticator = mock(Authenticator.class);
        final Principal principal = generateValidUser();

        when(authenticator.authenticate(any())).thenReturn(Optional.of(principal));

        return authenticator;
    }

    private static Principal generateValidUser() {
        final String username = TestHelpers.generateRandomString();
        return new PrincipalImpl(username);
    }

    private static Authorizer<Principal> generateAlwaysAuthorizedMock() {
        final Authorizer authorizer = mock(Authorizer.class);

        when(authorizer.authorize(any(), any())).thenReturn(true);

        return authorizer;
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorThrowsNullPointerExceptionWhenProvidedNullAuthenticator() throws AuthenticationException {
        createAuthFilterWithAuthenticator(null);
    }

    private static JsonWebTokenAuthFilter createAuthFilterWithAuthenticator(Authenticator<String, Principal> authenticator) {
        final Authorizer<Principal> authorizer = generateAlwaysAuthorizedMock();

        return createAuthFilter(authenticator, authorizer);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorThrowsNullPointerExceptionWhenProvidedNullAuthorizer() throws AuthenticationException {
        createAuthFilterWithAuthorizer(null);
    }

    private static JsonWebTokenAuthFilter createAuthFilterWithAuthorizer(Authorizer<Principal> authorizer) throws AuthenticationException {
        final Authenticator<String, Principal> authenticator = generateAlwaysAuthenticatesMock();

        return createAuthFilter(authenticator, authorizer);
    }

    // Filter

    @Test(expected = NullPointerException.class)
    public void testFilterThrowsNullPointerExceptionWhenProvidedWithNullContainerRequestContext() throws AuthenticationException, IOException {
        final JsonWebTokenAuthFilter filter = createValidAuthFilterInstance();

        // Should throw
        filter.filter(null);
    }

    @Test
    public void testFilterDoesNotThrowWhenProvidedWithDefaultDummyArguments() throws AuthenticationException, IOException {
        final JsonWebTokenAuthFilter filter = createValidAuthFilterInstance();
        final ContainerRequestContext request = createDummyRequest();

        // Shouldn't throw.
        filter.filter(request);
    }

    private static ContainerRequestContext createDummyRequest() {
        final String authorizationHeader = createDummyAuthorizationHeader();

        return createRequestWithAuthorizationHeader(authorizationHeader);
    }

    private static String createDummyAuthorizationHeader() {
        return createAuthorizationHeader(EXAMPLE_JWT_STRING);
    }

    private static String createAuthorizationHeader(String jwtToken) {
        return "Bearer " + jwtToken;
    }

    private static ContainerRequestContext createRequestWithAuthorizationHeader(String authHeaderValue) {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", authHeaderValue);
        return new MockRequestContext("GET", headers);
    }

    @Test(expected = WebApplicationException.class)
    public void testFilterThrowsWebApplicationExceptionIfAuthenticatorThrowsAuthenticationException() throws AuthenticationException, IOException {
        final Authenticator<String, Principal> authenticator = mock(Authenticator.class);
        when(authenticator.authenticate(any())).thenThrow(new AuthenticationException("Cannot authenticate"));

        final JsonWebTokenAuthFilter filter = createAuthFilterWithAuthenticator(authenticator);

        final ContainerRequestContext request = createDummyRequest();

        filter.filter(request);
    }

    @Test(expected = WebApplicationException.class)
    public void testFilterThrowsWebApplicationExceptionIfAuthenticatorReturnsAnEmptyOptional() throws AuthenticationException, IOException {
        final Authenticator<String, Principal> authenticator = mock(Authenticator.class);
        when(authenticator.authenticate(any())).thenReturn(Optional.empty());

        final JsonWebTokenAuthFilter filter = createAuthFilterWithAuthenticator(authenticator);

        final ContainerRequestContext request = createDummyRequest();

        filter.filter(request);
    }

    @Test(expected = WebApplicationException.class)
    public void testFilterThrowsWebApplicationExceptionIfRequestDoesNotHaveAnAuthenticationHeader() throws AuthenticationException, IOException {
        final JsonWebTokenAuthFilter filter = createValidAuthFilterInstance();

        final ContainerRequestContext request = createRequestWithAuthorizationHeader(null);

        filter.filter(request);
    }

    @Test
    public void testFilterSetsSecurityContextIfAuthenticationSuccessful() throws AuthenticationException, IOException {
        final JsonWebTokenAuthFilter filter = createValidAuthFilterInstance();

        final ContainerRequestContext request = createDummyRequest();

        // Sanity
        assertThat(request.getSecurityContext()).isNull();

        filter.filter(request);

        assertThat(request.getSecurityContext()).isNotNull();
    }

    @Test
    public void testSecurityContextIsAssignedToPrincipalReturnedByAuthenticator() throws AuthenticationException, IOException {
        final String username = TestHelpers.generateRandomString();
        final Principal injectedPrincipal = new PrincipalImpl(username);

        final Authenticator<String, Principal> authenticator = mock(Authenticator.class);
        when(authenticator.authenticate(any())).thenReturn(Optional.of(injectedPrincipal));

        final JsonWebTokenAuthFilter filter = createAuthFilterWithAuthenticator(authenticator);

        final ContainerRequestContext request = createDummyRequest();

        filter.filter(request);

        final SecurityContext securityContext = request.getSecurityContext();

        final String returnedName = securityContext.getUserPrincipal().getName();

        assertThat(returnedName).isEqualTo(username);
    }

    @Test
    public void testSecurityContextUsesProvidedAuthorizer() throws AuthenticationException, IOException {
        final Authorizer<Principal> authorizer = mock(Authorizer.class);
        when(authorizer.authorize(any(), any())).thenReturn(true);

        final JsonWebTokenAuthFilter filter = createAuthFilterWithAuthorizer(authorizer);

        final ContainerRequestContext request = createDummyRequest();

        filter.filter(request);

        final SecurityContext securityContext = request.getSecurityContext();

        verify(authorizer, times(0)).authorize(any(), any());

        final String role = TestHelpers.generateRandomString();

        securityContext.isUserInRole(role);

        verify(authorizer, times(1)).authorize(any(), any());
    }

    @Test
    public void testFilterCallsAuthenticatorWithTheJwtToken() throws AuthenticationException, IOException {
        final Authenticator<String, Principal> authenticator = generateAlwaysAuthenticatesMock();

        final JsonWebTokenAuthFilter<Principal> filter = createAuthFilterWithAuthenticator(authenticator);

        final String authHeader = createAuthorizationHeader(EXAMPLE_JWT_STRING);

        final ContainerRequestContext request = createRequestWithAuthorizationHeader(authHeader);

        filter.filter(request);

        verify(authenticator, times(1)).authenticate(EXAMPLE_JWT_STRING);
    }
}