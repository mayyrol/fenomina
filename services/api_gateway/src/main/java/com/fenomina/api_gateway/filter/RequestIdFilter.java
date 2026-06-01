package com.fenomina.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.http.HttpHeaders;

import java.util.UUID;

@Component
@Slf4j
public class RequestIdFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = exchange.getRequest()
                .getHeaders()
                .getFirst(REQUEST_ID_HEADER);

        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        final String finalRequestId = requestId;

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(REQUEST_ID_HEADER, finalRequestId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .response(new ServerHttpResponseDecorator(exchange.getResponse()) {
                    @Override
                    public HttpHeaders getHeaders() {
                        HttpHeaders headers = super.getHeaders();
                        headers.addIfAbsent(REQUEST_ID_HEADER, finalRequestId);
                        return headers;
                    }
                })
                .build();

        log.debug("Request-ID: {}", finalRequestId);

        return chain.filter(mutatedExchange);
    }
    @Override
    public int getOrder() {
        return 0;
    }
}