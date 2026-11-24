package com.ga.api_gateway.filter;

import com.ga.api_gateway.dto.response.AuthValidationResponse;
import com.ga.api_gateway.service.AuthValidationService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
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
 * validation to the User Service. Public authentication endpoints,
 * the health endpoint and CORS preflight requests bypass token
 * validation.</p>
 *
 * <p>For protected requests, the filter validates the JWT and forwards
 * the request with trusted user identity headers containing the
 * authenticated user's ID, role, and email.</p>
 *
 * <p>An internal secret is also forwarded to downstream services
 * to establish trust between the Gateway and those services.</p>
 *
 * <p>Invalid or missing tokens result in an HTTP 401 response.
 * If the User Service cannot complete validation, an HTTP 503
 * response is returned.</p>
 */
@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {

    private final AuthValidationService authValidationService;

    /**
     * Shared internal secret used to authenticate requests
     * forwarded from the Gateway to downstream services.
     */
    @Value("${app.internal-auth.secret}")
    private String internalAuthSecret;

    /**
     * Creates the Gateway authentication filter.
     *
     * @param authValidationService service responsible for validating JWTs
     */
    public GatewayAuthFilter(AuthValidationService authValidationService) {
        this.authValidationService = authValidationService;
    }

    /**
     * Endpoints that can be accessed without JWT authentication.
     *
     * <p>Includes the health endpoint so that the Elastic Beanstalk load
     * balancer can reach it without credentials.</p>
     */
    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
            "/auth/users/login",
            "/auth/users/register",
            "/auth/users/verify",
            "/auth/users/resend-verification",
            "/auth/users/forgot-password",
            "/auth/users/reset-password",
            "/actuator/health"
    );

    /**
     * Filters incoming Gateway requests and enforces authentication
     * for all endpoints except public authentication endpoints
     * and CORS preflight requests.
     *
     * @param exchange the current server web exchange
     * @param chain the Gateway filter chain
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
                || HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
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
     * Adds the authenticated user's identity and the internal secret
     * to the request headers before forwarding it to the downstream service.
     *
     * <p>Any identity or internal authentication headers supplied by
     * the original client are removed before trusted values are added.</p>
     *
     * @param exchange the current server web exchange
     * @param chain the Gateway filter chain
     * @param authResponse the authenticated user's identity returned by User Service
     * @return a Mono representing completion of the forwarded request
     */
    private Mono<Void> forwardAuthenticatedRequest(
            ServerWebExchange exchange,
            org.springframework.cloud.gateway.filter.GatewayFilterChain chain,
            AuthValidationResponse authResponse) {

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> {

                    // Remove identity and internal headers supplied by clients.
                    headers.remove("X-Authenticated-User-Id");
                    headers.remove("X-Authenticated-User-Role");
                    headers.remove("X-Authenticated-User-Email");
                    headers.remove("X-Internal-Secret");

                    // Add trusted identity information from User Service.
                    headers.set("X-Authenticated-User-Id",
                            String.valueOf(authResponse.userId()));

                    headers.set("X-Authenticated-User-Role",
                            authResponse.role());

                    headers.set("X-Authenticated-User-Email",
                            authResponse.email());

                    // Add the internal secret for downstream authentication.
                    headers.set("X-Internal-Secret", internalAuthSecret);
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
