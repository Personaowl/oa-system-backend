package com.personaowl.oa.common.security;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenServiceTest {
    @Test
    void issuesAndParsesToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("b2Etc3lzdGVtLXRlc3Qtc2VjcmV0LW11c3QtYmUtMzItYnl0ZXMtbG9uZw==");
        JwtTokenService service = new JwtTokenService(properties);

        String token = service.issue(new JwtClaims(
                1L, "admin", Set.of("ADMIN"), Set.of("sys:user:list"), null));
        JwtClaims claims = service.parse(token);

        assertEquals(1L, claims.userId());
        assertEquals("admin", claims.username());
        assertTrue(claims.permissions().contains("sys:user:list"));
    }
}

