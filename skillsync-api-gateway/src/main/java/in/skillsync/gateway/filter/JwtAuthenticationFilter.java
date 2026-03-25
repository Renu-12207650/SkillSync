package in.skillsync.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;

/**
 * JWT Authentication Filter for API Gateway.
 * Uses reactive GlobalFilter (WebFlux) — NOT servlet-based OncePerRequestFilter.
 * Validates Bearer JWT token on every request except open endpoints.
 * On success: injects X-User-Id and X-User-Role headers for downstream services.
 * On failure: returns 401 Unauthorized immediately.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret:skillsync-secret-key-capgemini-2025-very-long-secure-key}")
    private String jwtSecret;

    /**
     * Endpoints that do NOT require JWT validation.
     */
    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/auth/register",
            "/auth/login",
            "/auth/refresh",
            "/v3/api-docs",
            "/swagger-ui",
            "/actuator"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Gateway received request: {}", path);

        // Skip JWT validation for open endpoints
        if (isOpenEndpoint(path)) {
            log.debug("Open endpoint — skipping JWT validation: {}", path);
            return chain.filter(exchange);
        }

        // Extract Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            return sendUnauthorized(exchange, "Missing or malformed Authorization header");
        }

        String token = authHeader.substring(7);

        try {
            Key signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userId = claims.getSubject();
            String role   = claims.get("role", String.class);
            String email  = claims.get("email", String.class);

            log.debug("JWT valid — userId: {}, role: {}", userId, role);

            // Inject user context headers for downstream services
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id",    userId)
                    .header("X-User-Role",  role)
                    .header("X-User-Email", email)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JwtException e) {
            log.warn("Invalid JWT token for path {}: {}", path, e.getMessage());
            return sendUnauthorized(exchange, "Invalid or expired JWT token");
        } catch (Exception e) {
            log.error("Unexpected error during JWT validation: ", e);
            return sendUnauthorized(exchange, "Authentication error");
        }
    }

    @Override
    public int getOrder() {
        return -100; // Run before all other filters
    }

    private boolean isOpenEndpoint(String path) {
        return OPEN_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> sendUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String body = String.format(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\"}", message);
        var buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
