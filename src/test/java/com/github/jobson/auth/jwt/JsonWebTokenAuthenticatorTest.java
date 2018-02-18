package com.github.jobson.auth.jwt;

import com.github.jobson.TestHelpers;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.PrincipalImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import java.security.Key;
import java.security.Principal;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JsonWebTokenAuthenticatorTest {
    
    private static Key createSecretKey() {
        return MacProvider.generateKey();
    }

    private static SignatureAlgorithm getValidSignatureAlgorithm() {
        return SignatureAlgorithm.HS512;
    }

    private static JsonWebTokenAuthenticator createValidAuthenticatorInstance() {
        final Key secretKey = createSecretKey();
        final SignatureAlgorithm signatureAlgorithm = getValidSignatureAlgorithm();

        return new JsonWebTokenAuthenticator(secretKey, signatureAlgorithm);
    }

    private static JsonWebTokenAuthenticator createAuthenticatorWithSecretKey(Key secretKey) {
        final SignatureAlgorithm signatureAlgorithm = getValidSignatureAlgorithm();

        return new JsonWebTokenAuthenticator(secretKey, signatureAlgorithm);
    }

    private static JsonWebTokenAuthenticator createAuthenticatorWithSignatureAlgorithm(SignatureAlgorithm algorithm) {
        final Key secretKey = createSecretKey();

        return new JsonWebTokenAuthenticator(secretKey, algorithm);
    }

    private static JsonWebTokenAuthenticator createAuthenticator(Key secretKey, SignatureAlgorithm algorithm) {
        return new JsonWebTokenAuthenticator(secretKey, algorithm);
    }

    private static Principal generatePrincipal() {
        final String username = TestHelpers.generateRandomString();
        return new PrincipalImpl(username);
    }

    private static String createJwtToken(SignatureAlgorithm signatureAlgorithm, Key secretKey, Principal principal) {
        return Jwts.builder().setSubject(principal.getName()).signWith(signatureAlgorithm, secretKey).compact();
    }

    private static void assertIsValidJWT(Key secretKey, String jwtString) {
        try {
            Jwts.parser().setSigningKey(secretKey).parse(jwtString);
        } catch (MalformedJwtException ex) {
            Assert.fail(jwtString + ": not a valid JWT.");
        }
    }

    /**
     * Test that the ctor function works without throwing.
     */
    @Test
    public void testConstructorDoesNotThrowWithValidArguments() {
        // Shouldn't throw.
        createValidAuthenticatorInstance();
    }

    /**
     * Test that the ctor throws it provided a null key.
     */
    @Test(expected = NullPointerException.class)
    public void testConstructorThrowsIfProvidedANullSecretKey() {
        // Should throw because the secret key is null.
        createAuthenticatorWithSecretKey(null);
    }

    /**
     * Test that the ctor throws if provided a null signature algorithm.
     */
    @Test(expected = NullPointerException.class)
    public void testConstructorThrowsIfProvidedNullSignatureAlgorithm() {
        createAuthenticatorWithSignatureAlgorithm(null);
    }

    // .authenticate

    /**
     * Test that .authenticate throws if provided a null string.
     */
    @Test(expected = NullPointerException.class)
    public void testAuthenticateThrowsIfProvidedANullString() throws AuthenticationException {
        final JsonWebTokenAuthenticator authenticator = createValidAuthenticatorInstance();

        // Should throw, because you can't authenticate
        // against a null string.
        authenticator.authenticate(null);
    }

    /**
     * Test that .authenticate throws an AuthenticationException if provided
     * a mangled (non-JWT) string.
     */
    @Test(expected = AuthenticationException.class)
    public void testAuthenticateThrowsIfProvidedAnInvalidString() throws AuthenticationException {
        final JsonWebTokenAuthenticator authenticator = createValidAuthenticatorInstance();

        final String invalidString = TestHelpers.generateRandomString();

        authenticator.authenticate(invalidString);
    }

    /**
     * Test that .authenticate does not throw an error when provided with
     * a valid JWT token.
     */
    @Test
    public void testAuthenticateDoesNotThrowWHenProvidedWithAValidJWTToken() throws AuthenticationException {
        final Key secretKey = createSecretKey();
        final SignatureAlgorithm signatureAlgorithm = getValidSignatureAlgorithm();

        final Principal principal = generatePrincipal();

        final String jwt = createJwtToken(signatureAlgorithm, secretKey, principal);

        final JsonWebTokenAuthenticator authenticator = createAuthenticator(secretKey, signatureAlgorithm);

        // Shouldn't throw, because we created a valid jwt token
        // using the same secret key as the authenticator.
        authenticator.authenticate(jwt);
    }

    /**
     * Test that .authenticate does throw when provided with a valid
     * JWT created with a different secret key.
     */
    @Test(expected = AuthenticationException.class)
    public void testAuthenticateThrowsWhenProvidedWithAValidJWTCreatedFromADifferentSecretKey() throws AuthenticationException {
        final Key jwtKey = createSecretKey();
        final SignatureAlgorithm signatureAlgorithm = getValidSignatureAlgorithm();
        final Principal principal = generatePrincipal();

        final String jwt = createJwtToken(signatureAlgorithm, jwtKey, principal);

        final Key authenticatorKey = createSecretKey();

        final JsonWebTokenAuthenticator authenticator = createAuthenticator(authenticatorKey, signatureAlgorithm);

        // Should throw because jwt was created with a different secret
        // key.
        authenticator.authenticate(jwt);
    }


    /**
     * Test that .authenticate returns the provided (JWT-encoded) user
     * principal upon success.
     */
    @Test
    public void testAuthenticateReturnsTheProvidedJWTEncodedUserPricipalUponSuccess() throws AuthenticationException {
        final Key key = createSecretKey();
        final SignatureAlgorithm signatureAlgorithm = getValidSignatureAlgorithm();
        final Principal providedPrincipal = generatePrincipal();

        final String jwt = createJwtToken(signatureAlgorithm, key, providedPrincipal);

        final JsonWebTokenAuthenticator authenticator = createAuthenticator(key, signatureAlgorithm);

        final Optional<Principal> possibleAuthenticatedPrincipal = authenticator.authenticate(jwt);

        Assertions.assertThat(possibleAuthenticatedPrincipal).isNotNull();
        assertThat(possibleAuthenticatedPrincipal.isPresent()).isTrue();

        final Principal authenticatedPrincipal = possibleAuthenticatedPrincipal.get();

        assertThat(authenticatedPrincipal).isNotNull();
        assertThat(authenticatedPrincipal).isEqualTo(providedPrincipal);
    }

    // .createJwtToken

    /**
     * Test that .createJwtToken throws if user is null.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateJwtTokenThrowsIfUserIsNull() {
        final JsonWebTokenAuthenticator authenticator = createValidAuthenticatorInstance();

        // Should throw because the argument is null.
        authenticator.createJwtToken(null);
    }

    /**
     * Test that .createJwtToken does not return null.
     */
    @Test
    public void testCreateJwtTokenDoesNotReturnNull() {
        final Principal Principal = generatePrincipal();
        final JsonWebTokenAuthenticator authenticator = createValidAuthenticatorInstance();

        final String returnedToken = authenticator.createJwtToken(Principal);

        assertThat(returnedToken).isNotNull();
    }

    /**
     * Test that .createJwtToken returns a valid JWT string.
     */
    @Test
    public void testCreateJwtTokenReturnsValidJWTString() {
        final Key secretKey = createSecretKey();
        final Principal Principal = generatePrincipal();
        final JsonWebTokenAuthenticator authenticator = createAuthenticatorWithSecretKey(secretKey);

        final String returnedToken = authenticator.createJwtToken(Principal);

        assertIsValidJWT(secretKey, returnedToken);
    }

    // .createJwtToken and .authenticate cross-coupling

    /**
     * Test that a token created with .createJwtToken can be used in
     * .authenticate without throwing any exceptions.
     */
    @Test
    public void testUsingCreatedTokenInAuthenticateDoesNotThrow() throws AuthenticationException {
        final Principal Principal = generatePrincipal();
        final JsonWebTokenAuthenticator authenticator = createValidAuthenticatorInstance();

        final String token = authenticator.createJwtToken(Principal);

        // Shouldn't throw.
        authenticator.authenticate(token);
    }

    /**
     * Test that a token created with .createJwtToken returns the same
     * principal as provided when put through .authorize.
     */
    @Test
    public void testPrincipalUsedToCreateJWTIsReturnedByAuthenticate() throws AuthenticationException {
        final Principal suppliedPrincipal = generatePrincipal();
        final JsonWebTokenAuthenticator authenticator = createValidAuthenticatorInstance();

        final String token = authenticator.createJwtToken(suppliedPrincipal);

        final Optional<Principal> possibleReturnedUser = authenticator.authenticate(token);

        assertThat(possibleReturnedUser.isPresent());

        final Principal returnedPrincipal = possibleReturnedUser.get();

        assertThat(returnedPrincipal).isEqualTo(suppliedPrincipal);
    }
}