package in.skillsync.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("API Gateway: JWT Authentication Filter Tests")
class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;

    @Mock
    private GatewayFilterChain chain;

    private static final String SECRET = "skillsync-secret-key-capgemini-2025-very-long-secure-key";

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter();
        // Inject the secret key directly, bypassing Spring's @Value context
        ReflectionTestUtils.setField(filter, "jwtSecret", SECRET);
    }

    // ==========================================
    // 1. Open Endpoints Test
    // ==========================================

    @Test
    @DisplayName("Open Endpoints bypass JWT validation")
    void filter_OpenEndpoint_CallsChainDirectly() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain, times(1)).filter(exchange);
    }

    // ==========================================
    // 2. Missing or Malformed Header Tests
    // ==========================================

    @Test
    @DisplayName("Missing Authorization Header returns 401")
    void filter_MissingAuthHeader_ReturnsUnauthorized() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/me").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        MockServerHttpResponse response = exchange.getResponse();
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Malformed Authorization Header returns 401")
    void filter_MalformedAuthHeader_ReturnsUnauthorized() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Basic invalidTokenFormat")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    // ==========================================
    // 3. Valid Token Test (Happy Path)
    // ==========================================

    @Test
    @DisplayName("Valid JWT mutates request headers and proceeds")
    void filter_ValidJwt_MutatesHeadersAndCallsChain() {
        // Arrange
        String validToken = generateTestToken("user123", "LEARNER", "test@skillsync.in");

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert: Capture the mutated exchange passed to the chain
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        ServerWebExchange mutatedExchange = captor.getValue();
        HttpHeaders headers = mutatedExchange.getRequest().getHeaders();

        // Verify the downstream headers were successfully injected
        assertEquals("user123", headers.getFirst("X-User-Id"));
        assertEquals("LEARNER", headers.getFirst("X-User-Role"));
        assertEquals("test@skillsync.in", headers.getFirst("X-User-Email"));
    }

    // ==========================================
    // 4. Exception Handling Tests
    // ==========================================

    @Test
    @DisplayName("Invalid JWT token catches JwtException and returns 401")
    void filter_InvalidJwt_ReturnsUnauthorized() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer this.is.an.invalid.token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Unexpected Exception catches generic Exception and returns 401")
    void filter_UnexpectedException_ReturnsUnauthorized() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer validFormatButWeForceAnError")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Force a NullPointerException inside the try block by removing the secret
        ReflectionTestUtils.setField(filter, "jwtSecret", null);

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    // ==========================================
    // 5. Filter Order Test
    // ==========================================

    @Test
    @DisplayName("Filter order is set to -100")
    void getOrder_ReturnsCorrectValue() {
        assertEquals(-100, filter.getOrder());
    }

    // ==========================================
    // Helper Method
    // ==========================================

    private String generateTestToken(String subject, String role, String email) {
        Key signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .claim("email", email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hour
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
}