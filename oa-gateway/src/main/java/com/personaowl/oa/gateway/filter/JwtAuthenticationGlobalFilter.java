package com.personaowl.oa.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.common.core.web.RequestHeaders;
import com.personaowl.oa.common.security.JwtClaims;
import com.personaowl.oa.common.security.JwtTokenService;
import com.personaowl.oa.gateway.config.GatewaySecurityProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final GatewaySecurityProperties properties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationGlobalFilter(JwtTokenService jwtTokenService,
                                         GatewaySecurityProperties properties,
                                         ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (properties.getPublicPaths().stream().anyMatch(pattern -> pathMatcher.match(pattern, path))) {
            return chain.filter(clearIdentityHeaders(exchange));
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, "缺少或无效的访问令牌");
        }

        try {
            JwtClaims claims = jwtTokenService.parse(authorization.substring(BEARER_PREFIX.length()));
            ServerWebExchange authenticated = clearIdentityHeaders(exchange).mutate()
                    .request(builder -> builder.headers(headers -> {
                        headers.set(RequestHeaders.USER_ID, String.valueOf(claims.userId()));
                        headers.set(RequestHeaders.USERNAME, claims.username());
                        headers.set(RequestHeaders.ROLES, join(claims.roles()));
                        headers.set(RequestHeaders.PERMISSIONS, join(claims.permissions()));
                    }))
                    .build();
            return chain.filter(authenticated);
        } catch (RuntimeException exception) {
            return unauthorized(exchange, "访问令牌已过期或无法验证");
        }
    }

    private ServerWebExchange clearIdentityHeaders(ServerWebExchange exchange) {
        return exchange.mutate().request(builder -> builder.headers(headers -> {
            headers.remove(RequestHeaders.USER_ID);
            headers.remove(RequestHeaders.USERNAME);
            headers.remove(RequestHeaders.ROLES);
            headers.remove(RequestHeaders.PERMISSIONS);
        })).build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String traceId = exchange.getRequest().getHeaders().getFirst(RequestHeaders.TRACE_ID);
        ApiResponse<Void> response = ApiResponse.failure(ErrorCode.UNAUTHORIZED.code(), message, traceId);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(response);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException exception) {
            byte[] bytes = "{\"code\":\"A0103\",\"message\":\"Unauthorized\"}"
                    .getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        }
    }

    private String join(Collection<String> values) {
        return values == null ? "" : String.join(",", values);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
