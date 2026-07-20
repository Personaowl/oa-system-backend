package com.personaowl.oa.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class JwtTokenService {
    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenService(JwtProperties properties) {
        if (properties.getSecret() == null || properties.getSecret().isBlank()) {
            throw new IllegalArgumentException("oa.security.jwt.secret must be configured");
        }
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
    }

    public String issue(JwtClaims claims) {
        Instant now = Instant.now();
        String tokenId = claims.tokenId() == null || claims.tokenId().isBlank()
                ? UUID.randomUUID().toString()
                : claims.tokenId();
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(claims.username())
                .id(tokenId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getAccessTokenTtl())))
                .claims(Map.of(
                        "userId", claims.userId(),
                        "roles", claims.roles(),
                        "permissions", claims.permissions()))
                .signWith(key)
                .compact();
    }

    public JwtClaims parse(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Number userId = claims.get("userId", Number.class);
        return new JwtClaims(
                userId.longValue(),
                claims.getSubject(),
                asStringSet(claims.get("roles")),
                asStringSet(claims.get("permissions")),
                claims.getId());
    }

    private Set<String> asStringSet(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        collection.stream().map(String::valueOf).forEach(result::add);
        return Set.copyOf(result);
    }
}

