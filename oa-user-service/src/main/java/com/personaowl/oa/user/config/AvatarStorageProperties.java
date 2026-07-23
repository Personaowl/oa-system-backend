package com.personaowl.oa.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "oa.storage.avatar")
public class AvatarStorageProperties {
    private String directory = "D:/oa-system/data/avatars";
    private long maxBytes = 1024 * 1024;

    public String getDirectory() { return directory; }
    public void setDirectory(String directory) { this.directory = directory; }
    public long getMaxBytes() { return maxBytes; }
    public void setMaxBytes(long maxBytes) { this.maxBytes = maxBytes; }
}
