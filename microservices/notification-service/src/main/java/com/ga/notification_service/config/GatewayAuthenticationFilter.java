package com.ga.notification_service.config;

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
import java.util.List;

@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    @Value("${app.internal-auth.secret}")
    private String internalAuthSecret;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String secret = request.getHeader("X-Internal-Secret");
        String userId = request.getHeader("X-Authenticated-User-Id");
        String role = request.getHeader("X-Authenticated-User-Role");

        if (secret != null
                && secret.equals(internalAuthSecret)
                && userId != null
                && role != null) {

            try {
                Long authenticatedUserId = Long.parseLong(userId);

                var authentication =
                        new UsernamePasswordAuthenticationToken(
                                authenticatedUserId,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

            } catch (NumberFormatException ex) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
