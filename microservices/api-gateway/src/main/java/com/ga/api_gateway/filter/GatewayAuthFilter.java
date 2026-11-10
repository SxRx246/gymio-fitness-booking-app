package com.ga.api_gateway.filter;

import com.ga.api_gateway.dto.AuthValidationResponse;
import com.ga.api_gateway.service.AuthValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Global authentication filter for the API Gateway.
 *
 * <p>This filter centralizes JWT authentication by delegating token
 * validation to the User Service. Public authentication endpoints
 * and CORS preflight requests bypass token validation.</p>
 *
 * <p>For protected requests, the filter validates the JWT and forwards
 * the request with trusted user identity headers containing the
 * authenticated user's ID, role, and email.</p>
 *
 * <p>Invalid or missing tokens result in an HTTP 401 response.
 * If the User Service cannot complete validation, an HTTP 503
 * response is returned.</p>
 */
@Component
@RequiredArgsConstructor
public class GatewayAuthFilter implements GlobalFilter, Ordered {

    private final AuthValidationService authValidationService;

    /**
     * Endpoints that can be accessed without JWT authentication.
     */
    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
            "/auth/users/login",
            "/auth/users/register",
            "/auth/users/verify",
            "/auth/users/resend-verification",
            "/auth/users/forgot-password",
            "/auth/users/reset-password"
    );

    /**
     * Filters incoming gateway requests and enforces authentication
     * for all endpoints except public authentication endpoints
     * and CORS preflight requests.
     *
     * @param exchange the current server web exchange
     * @param chain the gateway filter chain
     * @return a Mono representing completion of the filtering process
     */
    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        String path = exchange.getRequest()
                .getPath()
                .pathWithinApplication()
                .value();

        // Allow public authentication endpoints and CORS preflight requests.
        if (PUBLIC_ENDPOINTS.contains(path)
                || exchange.getRequest().getMethod().name().equals("OPTIONS")) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        // Require a Bearer token for protected endpoints.
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String token = authorization.substring(7);

        return authValidationService.validateToken(token)
                .flatMap(authResponse -> forwardAuthenticatedRequest(
                        exchange, chain, authResponse))
                .onErrorResume(error -> {
                    if (error instanceof WebClientResponseException responseException
                            && responseException.getStatusCode().is4xxClientError()) {
                        return unauthorized(exchange);
                    }

                    return serviceUnavailable(exchange);
                });
    }

    /**
     * Adds the authenticated user's identity to the request headers
     * and forwards the request to the appropriate downstream service.
     *
     * <p>Any identity headers supplied by the original client are
     * removed before trusted values from User Service are added.</p>
     *
     * @param exchange the current server web exchange
     * @param chain the gateway filter chain
     * @param authResponse the authenticated user's identity
     * @return a Mono representing completion of the forwarded request
     */
    private Mono<Void> forwardAuthenticatedRequest(
            ServerWebExchange exchange,
            org.springframework.cloud.gateway.filter.GatewayFilterChain chain,
            AuthValidationResponse authResponse) {

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.remove("X-Authenticated-User-Id");
                    headers.remove("X-Authenticated-User-Role");
                    headers.remove("X-Authenticated-User-Email");

                    // Add identity information returned by User Service.
                    headers.set("X-Authenticated-User-Id",
                            String.valueOf(authResponse.userId()));

                    headers.set("X-Authenticated-User-Role",
                            authResponse.role());

                    headers.set("X-Authenticated-User-Email",
                            authResponse.email());
                })
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    /**
     * Returns an HTTP 401 Unauthorized response.
     *
     * @param exchange the current server web exchange
     * @return a Mono indicating completion of the response
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    /**
     * Returns an HTTP 503 Service Unavailable response when
     * the authentication validation service cannot be reached
     * or fails unexpectedly.
     *
     * @param exchange the current server web exchange
     * @return a Mono indicating completion of the response
     */
    private Mono<Void> serviceUnavailable(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        return exchange.getResponse().setComplete();
    }

    /**
     * Defines the execution order of this global filter.
     *
     * @return the filter order; negative values execute early
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
