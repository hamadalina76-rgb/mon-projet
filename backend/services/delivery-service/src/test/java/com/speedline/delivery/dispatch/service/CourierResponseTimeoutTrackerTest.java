package com.speedline.delivery.dispatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourierResponseTimeoutTrackerTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ZSetOperations<String, String> zSetOperations;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private CourierResponseTimeoutTracker tracker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        DispatchProperties properties = new DispatchProperties();
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        tracker = new CourierResponseTimeoutTracker(redisTemplate, new ObjectMapper(), properties);
    }

    @Test
    void trackAndRemoveProposal() {
        tracker.trackProposal(1L, 10L, Duration.ofSeconds(45));
        tracker.removeDeadline(1L, 10L);

        verify(zSetOperations).add(anyString(), anyString(), anyDouble());
        verify(zSetOperations).remove("dispatch:response:deadlines", "1:10");
    }

    @Test
    void findExpiredParsesRefs() {
        when(zSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(Set.of("1:10", "2:20"));

        var out = tracker.findExpired(Instant.now());

        assertEquals(2, out.size());
    }

    @Test
    void trackedOrderRoundTrip() {
        PendingOrder order = PendingOrder.builder().id(1L).zoneId(1L).build();
        tracker.trackProposal(1L, 10L, Duration.ofSeconds(45), order);

        when(valueOperations.get("dispatch:proposal:order:1")).thenReturn("{\"id\":1,\"zoneId\":1}");
        Optional<PendingOrder> out = tracker.getTrackedOrder(1L);

        assertTrue(out.isPresent());
        assertEquals(1L, out.get().getId());
    }
}
