package in.skillsync.common.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Security tests for JwtTokenProvider.
 * Verifies token generation, claim extraction, and validation logic.
 */
@DisplayName("JwtTokenProvider Security Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private static final String TEST_SECRET =
            "test-secret-key-for-unit-tests-must-be-long-enough-here";
    private static final Long USER_ID  = 1L;
    private static final String EMAIL  = "renu@skillsync.com";
    private static final String ROLE   = "ROLE_LEARNER";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret",         TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiryMs",  900_000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiryMs", 604_800_000L);
    }

    // ── Token generation ──────────────────────────────────────────────────────

    @Test
    @DisplayName("generateAccessToken - returns non-null token string")
    void generateAccessToken_returnsNonNullToken() {
        String token = jwtTokenProvider.generateAccessToken(USER_ID, EMAIL, ROLE);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("generateAccessToken - token has 3 parts separated by dots (JWT format)")
    void generateAccessToken_hasValidJwtStructure() {
        String token = jwtTokenProvider.generateAccessToken(USER_ID, EMAIL, ROLE);
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("generateRefreshToken - returns non-null token string")
    void generateRefreshToken_returnsNonNullToken() {
        String token = jwtTokenProvider.generateRefreshToken(USER_ID);
        assertThat(token).isNotNull().isNotBlank();
    }

    // ── Claim extraction ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserIdFromToken - extracts correct userId from access token")
    void getUserIdFromToken_extractsCorrectUserId() {
        String token = jwtTokenProvider.generateAccessToken(USER_ID, EMAIL, ROLE);
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("getRoleFromToken - extracts correct role from access token")
    void getRoleFromToken_extractsCorrectRole() {
        String token = jwtTokenProvider.generateAccessToken(USER_ID, EMAIL, ROLE);
        assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo(ROLE);
    }

    @Test
    @DisplayName("getEmailFromToken - extracts correct email from access token")
    void getEmailFromToken_extractsCorrectEmail() {
        String token = jwtTokenProvider.generateAccessToken(USER_ID, EMAIL, ROLE);
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("getUserIdFromToken - extracts correct userId from refresh token")
    void getUserIdFromToken_extractsCorrectUserIdFromRefreshToken() {
        String token = jwtTokenProvider.generateRefreshToken(USER_ID);
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(USER_ID);
    }

    // ── Token validation ──────────────────────────────────────────────────────

    @Test
    @DisplayName("isTokenValid - returns true for freshly generated valid token")
    void isTokenValid_returnsTrueForValidToken() {
        String token = jwtTokenProvider.generateAccessToken(USER_ID, EMAIL, ROLE);
        assertThat(jwtTokenProvider.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid - returns false for null token")
    void isTokenValid_returnsFalseForNull() {
        assertThat(jwtTokenProvider.isTokenValid(null)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid - returns false for empty string token")
    void isTokenValid_returnsFalseForEmptyString() {
        assertThat(jwtTokenProvider.isTokenValid("")).isFalse();
    }

    @Test
    @DisplayName("isTokenValid - returns false for completely invalid token")
    void isTokenValid_returnsFalseForGarbage() {
        assertThat(jwtTokenProvider.isTokenValid("not.a.jwt")).isFalse();
    }

    @Test
    @DisplayName("isTokenValid - returns false for token signed with wrong secret")
    void isTokenValid_returnsFalseForWrongSecret() {
        JwtTokenProvider otherProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(otherProvider, "jwtSecret",
                "completely-different-secret-key-that-is-long-enough");
        ReflectionTestUtils.setField(otherProvider, "accessTokenExpiryMs",  900_000L);
        ReflectionTestUtils.setField(otherProvider, "refreshTokenExpiryMs", 604_800_000L);

        String foreignToken = otherProvider.generateAccessToken(USER_ID, EMAIL, ROLE);
        assertThat(jwtTokenProvider.isTokenValid(foreignToken)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid - returns false for expired token")
    void isTokenValid_returnsFalseForExpiredToken() {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortLivedProvider, "jwtSecret",         TEST_SECRET);
        ReflectionTestUtils.setField(shortLivedProvider, "accessTokenExpiryMs",  -1000L); // already expired
        ReflectionTestUtils.setField(shortLivedProvider, "refreshTokenExpiryMs", -1000L);

        String expiredToken = shortLivedProvider.generateAccessToken(USER_ID, EMAIL, ROLE);
        assertThat(jwtTokenProvider.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("validateAndExtractClaims - throws JwtException for tampered token")
    void validateAndExtractClaims_throwsForTamperedToken() {
        String token = jwtTokenProvider.generateAccessToken(USER_ID, EMAIL, ROLE);
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "tamperedsignature";

        assertThatThrownBy(() -> jwtTokenProvider.validateAndExtractClaims(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("access and refresh tokens are different strings")
    void accessAndRefreshTokens_areDifferent() {
        String access  = jwtTokenProvider.generateAccessToken(USER_ID, EMAIL, ROLE);
        String refresh = jwtTokenProvider.generateRefreshToken(USER_ID);
        assertThat(access).isNotEqualTo(refresh);
    }

    @Test
    @DisplayName("two access tokens generated back-to-back are different (different iat)")
    void twoAccessTokens_areDifferent() throws InterruptedException {
        String first  = jwtTokenProvider.generateAccessToken(USER_ID, EMAIL, ROLE);
        Thread.sleep(1100);
        String second = jwtTokenProvider.generateAccessToken(USER_ID, EMAIL, ROLE);
        assertThat(first).isNotEqualTo(second);
    }
}
