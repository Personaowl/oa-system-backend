package com.personaowl.oa.user.service;

import com.personaowl.oa.common.redis.CacheNames;
import com.personaowl.oa.user.mapper.SysUserMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads authorization data through Redis. Keeping this in a separate bean makes
 * Spring's cache proxy effective when AuthService invokes it.
 */
@Service
public class AuthorizationCacheService {
    private final SysUserMapper userMapper;

    public AuthorizationCacheService(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Cacheable(cacheNames = CacheNames.USER_ROLES, key = "#userId", sync = true)
    public List<String> findRoleCodes(Long userId) {
        List<String> values = userMapper.findRoleCodes(userId);
        return values == null ? List.of() : new ArrayList<>(values);
    }

    @Cacheable(cacheNames = CacheNames.USER_PERMISSIONS, key = "#userId", sync = true)
    public List<String> findPermissionCodes(Long userId) {
        List<String> values = userMapper.findPermissionCodes(userId);
        return values == null ? List.of() : new ArrayList<>(values);
    }
}
