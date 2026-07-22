package com.personaowl.oa.attendance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    public Clock attendanceClock(AttendanceProperties properties) {
        return Clock.system(properties.resolvedZoneId());
    }
}
