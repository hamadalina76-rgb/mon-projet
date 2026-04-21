package com.speedline.delivery.dispatch.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

@Repository
@RequiredArgsConstructor
public class PartnerSlaCounterRepository {

    private static final String KEY_FORMAT = "partner:%d:sla:delays:%s";

    private final StringRedisTemplate redisTemplate;

    public long incrementMonthlyDelay(Long partnerId) {
        if (partnerId == null) {
            return 0L;
        }
        final String key = monthlyKey(partnerId, YearMonth.now(ZoneOffset.UTC));
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, ttlUntilMonthEnd());
        }
        return count == null ? 0L : count;
    }

    public long getMonthlyDelayCount(Long partnerId, YearMonth month) {
        if (partnerId == null || month == null) {
            return 0L;
        }
        String raw = redisTemplate.opsForValue().get(monthlyKey(partnerId, month));
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw);
        } catch (Exception ex) {
            return 0L;
        }
    }

    static String monthlyKey(Long partnerId, YearMonth month) {
        return KEY_FORMAT.formatted(partnerId, month);
    }

    private static Duration ttlUntilMonthEnd() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime monthEnd = now.with(TemporalAdjusters.lastDayOfMonth())
                .withHour(23)
                .withMinute(59)
                .withSecond(59)
                .withNano(0);
        Duration duration = Duration.between(now, monthEnd).plusDays(1);
        return duration.isNegative() ? Duration.ofDays(31) : duration;
    }
}
