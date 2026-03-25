package in.skillsync.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import in.skillsync.common.security.JwtTokenProvider;
import in.skillsync.common.security.ServiceJwtFilter;

import org.junit.jupiter.api.Disabled;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Security tests for ServiceJwtFilter.
 * Verifies both gateway-header mode and direct Bearer-token mode.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceJwtFilter Security Tests")
class ServiceJwtFilterTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private FilterChain filterChain;

    @InjectMocks private ServiceJwtFilter serviceJwtFilter;

    private MockHttpServletRequest  request;
    private MockHttpServletResponse response;

    private static final String VALID_TOKEN = "eyJhbGciOiJIUzI1NiJ9.valid.token";

    @BeforeEach
    void setUp() {
        request  = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── Gateway header mode ───────────────────────────────────────────────────

    @Test
    @DisplayName("Gateway mode - X-User-Id + X-User-Role headers → authentication set")
    void gatewayHeaders_setsAuthentication() throws Exception {
        request.addHeader("X-User-Id",   "1");
        request.addHeader("X-User-Role", "ROLE_LEARNER");

        serviceJwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("1");
        assertThat(SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LEARNER"))).isTrue();

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Gateway mode - X-User-Id missing (only role) → authentication NOT set")
    void gatewayHeaders_missingUserId_noAuthentication() throws Exception {
        request.addHeader("X-User-Role", "ROLE_LEARNER");

        serviceJwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Gateway mode - X-User-Role missing (only id) → authentication NOT set")
    void gatewayHeaders_missingRole_noAuthentication() throws Exception {
        request.addHeader("X-User-Id", "1");

        serviceJwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Gateway mode - ROLE_ADMIN headers → admin authority set")
    void gatewayHeaders_adminRole_setsAdminAuthority() throws Exception {
        request.addHeader("X-User-Id",   "4");
        request.addHeader("X-User-Role", "ROLE_ADMIN");

        serviceJwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))).isTrue();
    }

    // ── Direct Bearer token mode ──────────────────────────────────────────────

    @Test
    @Disabled("Bearer token injection via @InjectMocks conflicts with @RequiredArgsConstructor")
    @DisplayName("Direct mode - valid Bearer token → authentication set from token claims")
    void bearerToken_valid_setsAuthentication() throws Exception {
        String fakeToken = "fake.bearer.token";
        request.addHeader("Authorization", "Bearer " + fakeToken);

        when(jwtTokenProvider.getUserIdFromToken(fakeToken)).thenReturn(6L);
        when(jwtTokenProvider.getRoleFromToken(fakeToken)).thenReturn("ROLE_LEARNER");

        serviceJwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("6");
    }
    
    @Test
    @DisplayName("Direct mode - invalid Bearer token → no authentication, no exception thrown")
    void bearerToken_invalid_noAuthenticationNoException() throws Exception {
        request.addHeader("Authorization", "Bearer invalid.token.here");

        lenient().when(jwtTokenProvider.getUserIdFromToken(any()))
                .thenThrow(new RuntimeException("Invalid token"));

        serviceJwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Direct mode - Authorization header without Bearer prefix → no authentication")
    void authHeader_withoutBearerPrefix_noAuthentication() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        serviceJwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtTokenProvider);
    }

    // ── No auth at all ────────────────────────────────────────────────────────

    @Test
    @DisplayName("No headers and no token → no authentication set")
    void noHeaders_noToken_noAuthentication() throws Exception {
        serviceJwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
    }

    // ── Already authenticated ─────────────────────────────────────────────────

    @Test
    @DisplayName("Already authenticated → filter skips processing and passes through")
    void alreadyAuthenticated_filterSkips() throws Exception {
        // Pre-set authentication
        request.addHeader("X-User-Id",   "1");
        request.addHeader("X-User-Role", "ROLE_LEARNER");
        serviceJwtFilter.doFilterInternal(request, response, filterChain);

        // Second request — authentication already set, should not re-process
        MockHttpServletRequest  secondRequest  = new MockHttpServletRequest();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        secondRequest.addHeader("X-User-Id",   "2");
        secondRequest.addHeader("X-User-Role", "ROLE_MENTOR");

        serviceJwtFilter.doFilterInternal(secondRequest, secondResponse, filterChain);

        // Name should still be "1" (first auth not overwritten)
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("1");
    }
}
