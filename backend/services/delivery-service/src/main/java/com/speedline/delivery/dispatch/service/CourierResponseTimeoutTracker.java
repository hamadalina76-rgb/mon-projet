package com.speedline.delivery.dispatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourierResponseTimeoutTracker {

    private static final String DEADLINES_KEY = "dispatch:response:deadlines";
    private static final String PROPOSAL_ORDER_KEY = "dispatch:proposal:order:%d";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DispatchProperties properties;

    public void trackProposal(Long orderId, Long courierId, Duration timeout) {
        if (orderId == null || courierId == null || timeout == null) return;
        long deadlineMs = Instant.now().plus(timeout).toEpochMilli();
        redisTemplate.opsForZSet().add(DEADLINES_KEY, proposalMember(orderId, courierId), deadlineMs);
    }

    public void trackProposal(Long orderId, Long courierId, Duration timeout, PendingOrder pendingOrder) {
        trackProposal(orderId, courierId, timeout);
        if (orderId == null || pendingOrder == null) return;
        try {
            redisTemplate.opsForValue().set(
                    proposalOrderKey(orderId),
                    objectMapper.writeValueAsString(pendingOrder),
                    Duration.ofHours(properties.getPending().getTtlHours()));
        } catch (Exception ex) {
            log.warn("Unable to cache proposal order {}: {}", orderId, ex.getMessage());
        }
    }

    public void markResponded(Long orderId, Long courierId) {
        removeDeadline(orderId, courierId);
        if (orderId != null) {
            redisTemplate.delete(proposalOrderKey(orderId));
        }
    }

    public List<ProposalRef> findExpired(Instant now) {
        if (now == null) return List.of();
        Set<String> members = redisTemplate.opsForZSet().rangeByScore(DEADLINES_KEY, Double.NEGATIVE_INFINITY, now.toEpochMilli());
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        List<ProposalRef> out = new ArrayList<>(members.size());
        for (String member : members) {
            ProposalRef ref = parse(member);
            if (ref != null) out.add(ref);
        }
        return out;
    }

    public void removeDeadline(Long orderId, Long courierId) {
        if (orderId == null || courierId == null) return;
        redisTemplate.opsForZSet().remove(DEADLINES_KEY, proposalMember(orderId, courierId));
    }

    public Optional<PendingOrder> getTrackedOrder(Long orderId) {
        if (orderId == null) return Optional.empty();
        try {
            String json = redisTemplate.opsForValue().get(proposalOrderKey(orderId));
            if (json == null || json.isBlank()) return Optional.empty();
            return Optional.of(objectMapper.readValue(json, PendingOrder.class));
        } catch (Exception ex) {
            log.warn("Unable to read tracked order {}: {}", orderId, ex.getMessage());
            return Optional.empty();
        }
    }

    public void removeTrackedOrder(Long orderId) {
        if (orderId != null) {
            redisTemplate.delete(proposalOrderKey(orderId));
        }
    }

    private static String proposalMember(Long orderId, Long courierId) {
        return orderId + ":" + courierId;
    }

    private static String proposalOrderKey(Long orderId) {
        return String.format(PROPOSAL_ORDER_KEY, orderId);
    }

    private static ProposalRef parse(String value) {
        if (value == null || value.isBlank() || !value.contains(":")) return null;
        String[] parts = value.split(":", 2);
        try {
            return ProposalRef.builder()
                    .orderId(Long.parseLong(parts[0]))
                    .courierId(Long.parseLong(parts[1]))
                    .build();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class ProposalRef {
        private Long orderId;
        private Long courierId;
    }
}
