package com.personaowl.oa.user.api.dto;

public record LoginResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        CurrentUserResponse user
) {
}
