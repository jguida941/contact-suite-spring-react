package contactapp.security;

import contactapp.api.AuthController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that intercepts requests to validate JWT tokens.
 *
 * <p>Extracts the token from either:
 * <ol>
 *   <li>HttpOnly cookie (preferred, set by login/register endpoints)</li>
 *   <li>Authorization header (fallback for API clients)</li>
 * </ol>
 *
 * <p>Cookie-based auth is preferred for browser clients as it protects against XSS token theft.
 * Header-based auth is supported for programmatic API access (e.g., CI/CD, scripts).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(final JwtService jwtService, final UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull final HttpServletRequest request,
            @NonNull final HttpServletResponse response,
            @NonNull final FilterChain filterChain
    ) throws ServletException, IOException {
        // Try to extract JWT from cookie first, then fall back to Authorization header
        final String jwt = extractJwtFromCookie(request)
                .or(() -> extractJwtFromHeader(request))
                .orElse(null);

        // Skip if no JWT found
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String username = jwtService.extractUsername(jwt);

            final Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();

            if (username != null && existingAuth == null) {
                final UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    final UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token is invalid - continue without authentication
            // Security context remains empty, protected endpoints will return 401
            // Log constant message to avoid leaking validation details
            logger.debug("JWT validation failed");
            logger.trace("JWT validation error details", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from the auth cookie.
     *
     * @param request the HTTP request
     * @return Optional containing the JWT if found in cookie
     */
    private java.util.Optional<String> extractJwtFromCookie(final HttpServletRequest request) {
        final Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return java.util.Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> AuthController.AUTH_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isEmpty())
                .findFirst();
    }

    /**
     * Extracts JWT token from the Authorization header.
     *
     * @param request the HTTP request
     * @return Optional containing the JWT if found in header
     */
    private java.util.Optional<String> extractJwtFromHeader(final HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return java.util.Optional.of(authHeader.substring(BEARER_PREFIX.length()));
        }
        return java.util.Optional.empty();
    }
}
