package com.personaowl.oa.common.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "oa.cache.redis")
public class OaRedisCacheProperties {
    private Duration defaultTtl = Duration.ofMinutes(5);
    private Duration authorizationTtl = Duration.ofMinutes(15);
    private Duration organizationTtl = Duration.ofMinutes(5);
    private Duration rbacTtl = Duration.ofMinutes(10);
    private Duration noticeTtl = Duration.ofSeconds(30);

    public Duration getDefaultTtl() { return defaultTtl; }
    public void setDefaultTtl(Duration defaultTtl) { this.defaultTtl = defaultTtl; }
    public Duration getAuthorizationTtl() { return authorizationTtl; }
    public void setAuthorizationTtl(Duration authorizationTtl) { this.authorizationTtl = authorizationTtl; }
    public Duration getOrganizationTtl() { return organizationTtl; }
    public void setOrganizationTtl(Duration organizationTtl) { this.organizationTtl = organizationTtl; }
    public Duration getRbacTtl() { return rbacTtl; }
    public void setRbacTtl(Duration rbacTtl) { this.rbacTtl = rbacTtl; }
    public Duration getNoticeTtl() { return noticeTtl; }
    public void setNoticeTtl(Duration noticeTtl) { this.noticeTtl = noticeTtl; }
}
