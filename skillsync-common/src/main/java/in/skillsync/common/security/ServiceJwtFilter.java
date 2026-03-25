package in.skillsync.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT filter for all downstream microservices (NOT the API Gateway, NOT Auth Service).
 * Reads X-User-Id and X-User-Role headers injected by the API Gateway
 * after it has already validated the JWT token.
 * This allows @PreAuthorize annotations to work without re-validating JWT.
 *
 * Only active when spring.skillsync.service-jwt-filter.enabled=true
 * (set this in each downstream service's application.yml).
 * Auth Service does NOT set this property so this filter is skipped there.
 */
@Component
@ConditionalOnProperty(
        name = "skillsync.service-jwt-filter.enabled",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
public class ServiceJwtFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String role   = request.getHeader("X-User-Role");

        if (userId != null && role != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority(role))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
