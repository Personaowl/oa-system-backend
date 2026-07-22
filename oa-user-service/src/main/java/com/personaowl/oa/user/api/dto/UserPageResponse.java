package com.personaowl.oa.user.api.dto;

import java.util.List;

public record UserPageResponse(long total, List<UserResponse> records) {
}
