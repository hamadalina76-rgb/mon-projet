package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RefusalCounterRepository {

    private static final String DAILY_REFUSAL_KEY = "courier:%d:refusals:daily";
    private static final String ORDER_BLACKLIST_KEY = "courier:%d:blacklist:order:%d";

    private final StringRedisTemplate redisTemplate;
    private final DispatchProperties properties;

    public long incrementDailyRefusal(Long courierId) {
        String key = dailyRefusalKey(courierId);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofHours(properties.getRefusal().getCounterTtlHours()));
        }
        return count == null ? 0L : count;
    }

    public void blacklistOrderForCourier(Long orderId, Long courierId) {
        redisTemplate.opsForValue().set(
                blacklistKey(orderId, courierId),
                "1",
                Duration.ofSeconds(properties.getRefusal().getBlacklistTtlSeconds()));
    }

    public boolean isBlacklisted(Long orderId, Long courierId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey(orderId, courierId)));
    }

    public static String dailyRefusalKey(Long courierId) {
        return String.format(DAILY_REFUSAL_KEY, courierId);
    }

    public static String blacklistKey(Long orderId, Long courierId) {
        return String.format(ORDER_BLACKLIST_KEY, courierId, orderId);
    }
}
