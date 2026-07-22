package com.personaowl.oa.attendance.support;

import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public record OperatorContext(
        long userId,
        Set<String> permissions,
        String traceId
) {
    public OperatorContext {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public static OperatorContext fromHeaders(String userIdHeader, String permissionsHeader, String traceId) {
        if (!StringUtils.hasText(userIdHeader)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少可信用户身份");
        }

        long userId;
        try {
            userId = Long.parseLong(userIdHeader.trim());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "可信用户身份格式无效");
        }
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "可信用户身份格式无效");
        }

        Set<String> permissions = StringUtils.hasText(permissionsHeader)
                ? Arrays.stream(permissionsHeader.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet())
                : Set.of();
        return new OperatorContext(userId, permissions, traceId);
    }
}
