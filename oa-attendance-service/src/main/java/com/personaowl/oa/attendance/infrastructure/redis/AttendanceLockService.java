package com.personaowl.oa.attendance.infrastructure.redis;

import com.personaowl.oa.attendance.config.AttendanceProperties;
import com.personaowl.oa.common.redis.RedisKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class AttendanceLockService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceLockService.class);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] "
                    + "then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final AttendanceProperties properties;

    public AttendanceLockService(StringRedisTemplate redisTemplate, AttendanceProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public LockHandle acquire(long userId, LocalDate workDate) {
        if (!properties.isRedisLockEnabled()) {
            return LockHandle.bypass();
        }

        String key = RedisKeys.attendanceLock(userId, workDate);
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    key, token, Duration.ofSeconds(properties.getLockTtlSeconds()));
            return Boolean.TRUE.equals(acquired)
                    ? LockHandle.acquired(key, token)
                    : LockHandle.contended();
        } catch (RuntimeException exception) {
            log.warn("attendance.lock.unavailable userId={} workDate={} fallbackUsed=true cause={}",
                    userId, workDate, exception.toString());
            log.debug("Redis attendance lock failure", exception);
            return LockHandle.bypass();
        }
    }

    public void release(LockHandle handle) {
        if (handle == null || !handle.acquired()) {
            return;
        }
        try {
            redisTemplate.execute(RELEASE_SCRIPT, List.of(handle.key()), handle.token());
        } catch (RuntimeException exception) {
            log.warn("attendance.lock.release-failed lockKeyHash={} cause={}",
                    handle.key().hashCode(), exception.toString());
            log.debug("Redis attendance lock release failure", exception);
        }
    }

    public record LockHandle(String key, String token, boolean acquired, boolean bypassed) {

        public static LockHandle acquired(String key, String token) {
            return new LockHandle(key, token, true, false);
        }

        public static LockHandle contended() {
            return new LockHandle(null, null, false, false);
        }

        public static LockHandle bypass() {
            return new LockHandle(null, null, false, true);
        }

        public boolean allowsProceeding() {
            return acquired || bypassed;
        }
    }
}
