package com.personaowl.oa.attendance.support;

import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public record OperatorContext(
        long userId,
        Set<String> roles,
        Set<String> permissions,
        String traceId
) {
    public OperatorContext {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public OperatorContext(long userId, Set<String> permissions, String traceId) {
        this(userId, Set.of(), permissions, traceId);
    }

    public static OperatorContext fromHeaders(String userIdHeader, String permissionsHeader, String traceId) {
        return fromHeaders(userIdHeader, null, permissionsHeader, traceId);
    }

    public static OperatorContext fromHeaders(String userIdHeader,
                                              String rolesHeader,
                                              String permissionsHeader,
                                              String traceId) {
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

        Set<String> roles = parseValues(rolesHeader);
        Set<String> permissions = parseValues(permissionsHeader);
        return new OperatorContext(userId, roles, permissions, traceId);
    }

    private static Set<String> parseValues(String header) {
        return StringUtils.hasText(header)
                ? Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet())
                : Set.of();
    }
}
