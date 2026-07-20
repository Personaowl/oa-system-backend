package com.personaowl.oa.common.security;

import java.util.Set;

public record JwtClaims(
        Long userId,
        String username,
        Set<String> roles,
        Set<String> permissions,
        String tokenId
) {
}

