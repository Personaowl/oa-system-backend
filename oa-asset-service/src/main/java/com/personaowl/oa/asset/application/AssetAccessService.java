package com.personaowl.oa.asset.application;

import com.personaowl.oa.asset.domain.AssetUserScope;
import com.personaowl.oa.asset.infrastructure.AssetAccessMapper;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AssetAccessService {
    private final AssetAccessMapper accessMapper;
    public AssetAccessService(AssetAccessMapper accessMapper) { this.accessMapper = accessMapper; }

    @Transactional(readOnly = true)
    public AccessSnapshot resolve(Long userId, String roles, String permissions) {
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        AssetUserScope user = accessMapper.findUser(userId);
        if (user == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户不存在或已停用");
        boolean admin = containsAny(roles, "ADMIN", "ROLE_ADMIN", "SUPER_ADMIN") || contains(permissions, "system:admin");
        boolean hr = containsAny(roles, "HR", "ROLE_HR");
        boolean managerRole = containsAny(roles, "MANAGER", "ROLE_MANAGER");
        Set<Long> managed = new LinkedHashSet<>(accessMapper.findManagedDepartmentIds(userId));
        if (managerRole && user.getDepartmentId() != null) managed.add(user.getDepartmentId());
        return new AccessSnapshot(user, admin, hr, Set.copyOf(managed));
    }

    private boolean containsAny(String source, String... expected) {
        return Arrays.stream(expected).anyMatch(value -> contains(source, value));
    }

    private boolean contains(String source, String expected) {
        if (!StringUtils.hasText(source)) return false;
        String target = expected.toUpperCase(Locale.ROOT);
        return Arrays.stream(source.split(",")).map(String::trim).map(value -> value.toUpperCase(Locale.ROOT)).anyMatch(target::equals);
    }

    public record AccessSnapshot(AssetUserScope user, boolean admin, boolean hr, Set<Long> managedDepartmentIds) {
        public boolean inventoryManager() { return admin || hr; }
        public boolean departmentReviewer() { return inventoryManager() || !managedDepartmentIds.isEmpty(); }
        public boolean canReview(Long departmentId) { return inventoryManager() || managedDepartmentIds.contains(departmentId); }
        public boolean allAssetAccess() { return inventoryManager(); }
        public List<Long> visibleAssetDepartmentIds() { return List.copyOf(managedDepartmentIds); }
    }
}
