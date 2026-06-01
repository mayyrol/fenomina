package com.fenomina.api_gateway.filter;
import com.fenomina.api_gateway.config.GatewayAuthProperties;
import com.fenomina.api_gateway.security.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;

    private final GatewayAuthProperties gatewayAuthProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        log.debug("Gateway request: {} {}", method, path);

        if (isExcluded(path)) {
            log.debug("Ruta excluida de validación JWT: {}", path);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        String token = jwtUtils.extractToken(authHeader);

        if (token == null) {
            log.warn("Request sin token JWT: {} {}", method, path);
            return unauthorized(exchange, "Token no proporcionado");
        }

        try {
            Claims claims = jwtUtils.validateToken(token);

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .header("X-User-Id", String.valueOf(claims.get("userId")))
                            .header("X-User-Role", String.valueOf(claims.get("rol")))
                            .header("X-Username", claims.getSubject())
                            .build())
                    .build();

            log.debug("JWT válido — usuario: {}, rol: {}",
                    claims.getSubject(), claims.get("rol"));

            return chain.filter(mutatedExchange);

        } catch (JwtException e) {
            log.warn("JWT inválido en {} {}: {}", method, path, e.getMessage());
            return unauthorized(exchange, "Token inválido o expirado");
        }
    }

    private boolean isExcluded(String path) {
        return gatewayAuthProperties.getExcludedPaths().stream()
                .anyMatch(path::equals);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders()
                .add("X-Auth-Error", message);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
