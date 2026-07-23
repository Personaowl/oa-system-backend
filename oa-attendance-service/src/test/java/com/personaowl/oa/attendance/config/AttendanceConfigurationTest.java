package com.personaowl.oa.attendance.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceConfigurationTest {

    @Test
    void exposesFrozenDefaultRulesAsOneImmutableSnapshot() {
        AttendanceProperties properties = new AttendanceProperties();

        assertEquals(LocalTime.of(9, 0), properties.snapshot().workStart());
        assertEquals(LocalTime.of(18, 0), properties.snapshot().workEnd());
        assertEquals(5, properties.snapshot().lateThresholdMinutes());
        assertEquals(10, properties.getLockTtlSeconds());
        assertTrue(properties.isRedisLockEnabled());
    }

    @Test
    void createsClockInConfiguredZone() {
        AttendanceProperties properties = new AttendanceProperties();
        Clock clock = new ClockConfig().attendanceClock(properties);

        assertEquals(ZoneId.of("Asia/Shanghai"), clock.getZone());
    }

    @Test
    void createsRefreshScopedPropertiesAndClockInsideSpringContext() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(RefreshScope.class);
            context.register(AttendanceProperties.class, ClockConfig.class);
            context.refresh();

            assertEquals(ZoneId.of("Asia/Shanghai"), context.getBean(Clock.class).getZone());
            assertEquals(LocalTime.of(9, 0), context.getBean(AttendanceProperties.class).getWorkStart());
        }
    }
}
