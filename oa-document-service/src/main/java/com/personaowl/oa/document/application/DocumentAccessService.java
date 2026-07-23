package com.personaowl.oa.document.application;

import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.document.domain.DocumentUserScope;
import com.personaowl.oa.document.infrastructure.DocumentAccessMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Arrays;

@Service
public class DocumentAccessService {

    private final DocumentAccessMapper accessMapper;

    public DocumentAccessService(DocumentAccessMapper accessMapper) {
        this.accessMapper = accessMapper;
    }

    @Transactional(readOnly = true)
    public AccessSnapshot resolve(Long userId, String roles, String permissions) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        DocumentUserScope user = accessMapper.findUserScope(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户不存在或已停用");
        }

        boolean admin = contains(permissions, "system:admin")
            || containsAny(roles, "ADMIN", "ROLE_ADMIN", "SUPER_ADMIN");
        Set<Long> managedDepartmentIds = new LinkedHashSet<>(
            accessMapper.findManagedDepartmentIds(userId)
        );
        Set<Long> accessibleDepartmentIds = new LinkedHashSet<>();
        if (admin) {
            accessibleDepartmentIds.addAll(accessMapper.findAllDepartmentIds());
        } else {
            if (user.getDepartmentId() != null) {
                accessibleDepartmentIds.add(user.getDepartmentId());
            }
            accessibleDepartmentIds.addAll(managedDepartmentIds);
        }
        return new AccessSnapshot(
            user,
            admin,
            Set.copyOf(managedDepartmentIds),
            List.copyOf(accessibleDepartmentIds)
        );
    }

    private boolean containsAny(String source, String... expected) {
        for (String value : expected) {
            if (contains(source, value)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(String source, String expected) {
        if (!StringUtils.hasText(source)) {
            return false;
        }
        String normalizedExpected = expected.toUpperCase(Locale.ROOT);
        return Arrays.stream(source.split(","))
            .map(String::trim)
            .map(value -> value.toUpperCase(Locale.ROOT))
            .anyMatch(normalizedExpected::equals);
    }

    public record AccessSnapshot(
        DocumentUserScope user,
        boolean admin,
        Set<Long> managedDepartmentIds,
        List<Long> accessibleDepartmentIds
    ) {
        public boolean canAccess(Long departmentId) {
            return departmentId != null && accessibleDepartmentIds.contains(departmentId);
        }

        public boolean canManage(Long departmentId) {
            return departmentId != null
                && (admin || managedDepartmentIds.contains(departmentId));
        }
    }
}
