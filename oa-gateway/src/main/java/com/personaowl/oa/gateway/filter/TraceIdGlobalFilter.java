package com.personaowl.oa.gateway.filter;

import com.personaowl.oa.common.core.web.RequestHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(RequestHeaders.TRACE_ID);
        String traceId = StringUtils.hasText(incoming) ? incoming : UUID.randomUUID().toString().replace("-", "");

        exchange.getResponse().getHeaders().set(RequestHeaders.TRACE_ID, traceId);
        ServerWebExchange mutated = exchange.mutate()
                .request(builder -> builder.headers(headers -> headers.set(RequestHeaders.TRACE_ID, traceId)))
                .build();
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
