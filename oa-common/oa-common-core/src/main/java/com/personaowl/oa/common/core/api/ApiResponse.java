package com.personaowl.oa.common.core.api;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record ApiResponse<T>(
        String code,
        String message,
        T data,
        String traceId,
        OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>("0", "success", data, traceId, now());
    }

    public static <T> ApiResponse<T> failure(String code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId, now());
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.ofHours(8));
    }
}

