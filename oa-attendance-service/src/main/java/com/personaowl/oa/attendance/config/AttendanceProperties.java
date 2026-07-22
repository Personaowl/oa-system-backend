package com.personaowl.oa.attendance.config;

import com.personaowl.oa.attendance.domain.RuleSnapshot;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.DateTimeException;

@Component
@RefreshScope
@Validated
@ConfigurationProperties(prefix = "oa.attendance")
public class AttendanceProperties {

    @NotNull
    private LocalTime workStart = LocalTime.of(9, 0);

    @Min(0)
    private int lateThresholdMinutes = 5;

    @Min(1)
    private int lockTtlSeconds = 10;

    @NotNull
    private LocalTime workEnd = LocalTime.of(18, 0);

    @NotBlank
    private String zoneId = "Asia/Shanghai";

    private boolean redisLockEnabled = true;

    public RuleSnapshot snapshot() {
        return new RuleSnapshot(workStart, workEnd, lateThresholdMinutes);
    }

    public ZoneId resolvedZoneId() {
        return ZoneId.of(zoneId);
    }

    @AssertTrue(message = "oa.attendance.zone-id must be a valid time-zone ID")
    public boolean isValidZoneId() {
        try {
            ZoneId.of(zoneId);
            return true;
        } catch (DateTimeException | NullPointerException exception) {
            return false;
        }
    }

    public LocalTime getWorkStart() {
        return workStart;
    }

    public void setWorkStart(LocalTime workStart) {
        this.workStart = workStart;
    }

    public int getLateThresholdMinutes() {
        return lateThresholdMinutes;
    }

    public void setLateThresholdMinutes(int lateThresholdMinutes) {
        this.lateThresholdMinutes = lateThresholdMinutes;
    }

    public int getLockTtlSeconds() {
        return lockTtlSeconds;
    }

    public void setLockTtlSeconds(int lockTtlSeconds) {
        this.lockTtlSeconds = lockTtlSeconds;
    }

    public LocalTime getWorkEnd() {
        return workEnd;
    }

    public void setWorkEnd(LocalTime workEnd) {
        this.workEnd = workEnd;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public boolean isRedisLockEnabled() {
        return redisLockEnabled;
    }

    public void setRedisLockEnabled(boolean redisLockEnabled) {
        this.redisLockEnabled = redisLockEnabled;
    }
}
