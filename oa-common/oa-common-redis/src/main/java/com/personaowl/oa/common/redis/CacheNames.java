package com.personaowl.oa.common.redis;

/**
 * Redis cache names shared by the business services.
 */
public final class CacheNames {
    public static final String USER_ROLES = "user-roles";
    public static final String USER_PERMISSIONS = "user-permissions";
    public static final String DEPARTMENT_LIST = "department-list";
    public static final String DEPARTMENT_DETAIL = "department-detail";
    public static final String RBAC_ROLES = "rbac-roles";
    public static final String RBAC_PERMISSIONS = "rbac-permissions";
    public static final String NOTICE_UNREAD_COUNT = "notice-unread-count";

    private CacheNames() {
    }
}
