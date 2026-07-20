package com.personaowl.oa.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "oa.gateway.security")
public class GatewaySecurityProperties {

    private List<String> publicPaths = new ArrayList<>(List.of(
            "/api/v1/auth/login",
            "/actuator/health/**"
    ));

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }
}
