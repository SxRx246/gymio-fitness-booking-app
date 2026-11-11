package com.ga.fitness_booking_service.config;

import com.ga.fitness_booking_service.dto.AuthenticatedUser;
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

/**
 * Establishes user authentication using trusted identity headers
 * forwarded by the API Gateway.
 *
 * <p>The filter verifies the internal shared secret before accepting
 * the user identity supplied in the request headers.</p>
 *
 * <p>JWT validation is handled centrally by the API Gateway and User Service.
 * This filter does not independently validate JWT tokens.</p>
 */
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    @Value("${app.internal-auth.secret}")
    private String internalAuthSecret;

    /**
     * Processes incoming requests and establishes authentication
     * when valid Gateway identity headers are provided.
     *
     * @param request the incoming HTTP request
     * @param response the HTTP response
     * @param filterChain the remaining servlet filter chain
     * @throws ServletException if filter processing fails
     * @throws IOException if an input or output error occurs
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        SecurityContextHolder.clearContext();

        String secret = request.getHeader("X-Internal-Secret");
        String userId = request.getHeader("X-Authenticated-User-Id");
        String role = request.getHeader("X-Authenticated-User-Role");
        String email = request.getHeader("X-Authenticated-User-Email");

        if (secret != null
                && MessageDigest.isEqual(
                secret.getBytes(StandardCharsets.UTF_8),
                internalAuthSecret.getBytes(StandardCharsets.UTF_8))
                && userId != null
                && role != null
                && !role.isBlank()
                && email != null
                && !email.isBlank()) {

            try {
                Long authenticatedUserId = Long.parseLong(userId);

                if (authenticatedUserId <= 0) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                String normalizedRole = role.startsWith("ROLE_")
                        ? role.substring(5)
                        : role;

                AuthenticatedUser authenticatedUser =
                        new AuthenticatedUser(
                                authenticatedUserId,
                                normalizedRole,
                                email
                        );

                var authentication =
                        new UsernamePasswordAuthenticationToken(
                                authenticatedUser,
                                null,
                                List.of(
                                        new SimpleGrantedAuthority(
                                                "ROLE_" + normalizedRole
                                        )
                                )
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

            } catch (NumberFormatException exception) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
