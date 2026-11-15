package com.ga.membership_payment_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Authenticates requests forwarded by the Gymio API Gateway.
 * Validates the internal secret and extracts the authenticated user's ID
 * and role from trusted Gateway headers.
 */
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    private static final String USER_ID_HEADER = "X-Authenticated-User-Id";
    private static final String USER_ROLE_HEADER = "X-Authenticated-User-Role";

    private static final Set<String> ALLOWED_ROLES =
            Set.of("ADMIN", "CUSTOMER", "TRAINER");

    @Value("${app.internal-auth.secret}")
    private String internalSecret;

    /**
     * Determines whether the filter should skip the current request.
     *
     * @param request the incoming HTTP request
     * @return true for health checks and CORS preflight requests
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/actuator/health".equals(path)
                || "/actuator/info".equals(path);
    }

    /**
     * Validates the Gateway secret and authenticates the forwarded user.
     *
     * @param request the incoming HTTP request
     * @param response the HTTP response
     * @param filterChain the remaining security filter chain
     * @throws ServletException if filtering fails
     * @throws IOException if an input/output error occurs
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String suppliedSecret = request.getHeader(INTERNAL_SECRET_HEADER);
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        String roleHeader = request.getHeader(USER_ROLE_HEADER);

        if (!isValidSecret(suppliedSecret)) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized request."
            );
            return;
        }

        if (userIdHeader == null || roleHeader == null) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing authentication information."
            );
            return;
        }

        try {
            Long userId = Long.parseLong(userIdHeader);

            String role = roleHeader.trim().toUpperCase(Locale.ROOT);

            if (role.startsWith("ROLE_")) {
                role = role.substring(5);
            }

            if (userId <= 0 || !ALLOWED_ROLES.contains(role)) {
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid authentication information."
                );
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (NumberFormatException exception) {
            SecurityContextHolder.clearContext();

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid user ID."
            );
        }
    }

    /**
     * Compares the supplied internal secret with the configured secret.
     *
     * @param suppliedSecret the secret received in the request header
     * @return true if the supplied secret matches the configured secret
     */
    private boolean isValidSecret(String suppliedSecret) {
        if (suppliedSecret == null
                || internalSecret == null
                || internalSecret.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
                internalSecret.getBytes(StandardCharsets.UTF_8),
                suppliedSecret.getBytes(StandardCharsets.UTF_8)
        );
    }
}
