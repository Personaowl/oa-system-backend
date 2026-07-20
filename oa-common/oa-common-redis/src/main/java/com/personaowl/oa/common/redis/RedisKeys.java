package com.personaowl.oa.common.redis;

import java.time.LocalDate;

public final class RedisKeys {
    private static final String PREFIX = "oa:";

    private RedisKeys() {
    }

    public static String jwtBlacklist(String tokenId) {
        return PREFIX + "auth:blacklist:" + tokenId;
    }

    public static String userPermissions(long userId) {
        return PREFIX + "user:permission:" + userId;
    }

    public static String attendanceLock(long userId, LocalDate date) {
        return PREFIX + "attendance:lock:" + userId + ":" + date;
    }
}

