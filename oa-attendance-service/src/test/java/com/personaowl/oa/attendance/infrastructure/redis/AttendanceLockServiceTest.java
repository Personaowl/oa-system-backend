package com.personaowl.oa.attendance.infrastructure.redis;

import com.personaowl.oa.attendance.config.AttendanceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceLockServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AttendanceProperties properties;
    private AttendanceLockService lockService;

    @BeforeEach
    void setUp() {
        properties = new AttendanceProperties();
        lockService = new AttendanceLockService(redisTemplate, properties);
    }

    @Test
    void acquiresAndReleasesOnlyItsOwnToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                any(String.class), any(String.class), eq(Duration.ofSeconds(10))))
                .thenReturn(true);

        AttendanceLockService.LockHandle handle = lockService.acquire(10001L, LocalDate.of(2026, 7, 21));
        lockService.release(handle);

        assertTrue(handle.acquired());
        assertTrue(handle.allowsProceeding());
        verify(redisTemplate).execute(any(RedisScript.class), anyList(), eq(handle.token()));
    }

    @Test
    void reportsContentionWhenAnotherRequestOwnsTheLock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(String.class), any(String.class), any(Duration.class)))
                .thenReturn(false);

        AttendanceLockService.LockHandle handle = lockService.acquire(10001L, LocalDate.of(2026, 7, 21));

        assertFalse(handle.acquired());
        assertFalse(handle.bypassed());
        assertFalse(handle.allowsProceeding());
        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any());
    }

    @Test
    void bypassesRedisWhenItIsUnavailable() {
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis unavailable"));

        AttendanceLockService.LockHandle handle = lockService.acquire(10001L, LocalDate.of(2026, 7, 21));

        assertTrue(handle.bypassed());
        assertTrue(handle.allowsProceeding());
    }

    @Test
    void bypassesRedisWhenLockingIsDisabled() {
        properties.setRedisLockEnabled(false);

        AttendanceLockService.LockHandle handle = lockService.acquire(10001L, LocalDate.of(2026, 7, 21));

        assertTrue(handle.bypassed());
        verifyNoInteractions(redisTemplate);
    }
}
