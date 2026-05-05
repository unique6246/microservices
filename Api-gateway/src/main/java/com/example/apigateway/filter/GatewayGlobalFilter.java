package com.example.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global filter that:
 *  1. Stamps an X-Correlation-ID on every inbound request (if not already present)
 *  2. Propagates JWT sub claim as X-User-Id to downstream services
 *  3. Logs request entry/exit with latency
 */
@Slf4j
@Component
public class GatewayGlobalFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();

        // Stamp correlation ID
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        final String cid = correlationId;

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, cid)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        log.info("Gateway IN  method={} path={} correlationId={}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                cid);

        // Set the correlation ID on the response BEFORE it is committed (headers are writable here)
        mutatedExchange.getResponse().beforeCommit(() -> {
            mutatedExchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, cid);
            return Mono.empty();
        });

        return chain.filter(mutatedExchange)
                .doFinally(signalType -> {
                    long latency = System.currentTimeMillis() - startTime;
                    int status = mutatedExchange.getResponse().getStatusCode() != null
                            ? mutatedExchange.getResponse().getStatusCode().value() : 0;
                    log.info("Gateway OUT status={} latency={}ms correlationId={}", status, latency, cid);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

