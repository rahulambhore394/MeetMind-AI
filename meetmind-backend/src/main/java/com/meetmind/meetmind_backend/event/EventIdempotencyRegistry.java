package com.meetmind.meetmind_backend.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class EventIdempotencyRegistry {

    private static final Logger log = LoggerFactory.getLogger(EventIdempotencyRegistry.class);
    private final StringRedisTemplate redisTemplate;

    public EventIdempotencyRegistry(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks if the event has already been processed by a consumer group.
     * Returns true if it is a duplicate (already processed), false if not.
     */
    public boolean isDuplicate(String eventId, String consumerGroup) {
        String key = "event:processed:" + consumerGroup + ":" + eventId;
        try {
            // Set value to "1" only if the key does not exist, with a 24-hour TTL
            Boolean set = redisTemplate.opsForValue().setIfAbsent(key, "1", 24, TimeUnit.HOURS);
            boolean duplicate = (set == null || !set);
            if (duplicate) {
                log.warn("Duplicate event detected. EventId: {}, ConsumerGroup: {}", eventId, consumerGroup);
            }
            return duplicate;
        } catch (Exception e) {
            log.error("Redis error in EventIdempotencyRegistry. Falling back to fail-open processing.", e);
            // Fail-open: If Redis is unavailable, assume not duplicate to ensure processing
            return false;
        }
    }

    /**
     * Removes the event tracking key from Redis.
     * This is useful to allow retries if a consumer fails mid-flight.
     */
    public void remove(String eventId, String consumerGroup) {
        String key = "event:processed:" + consumerGroup + ":" + eventId;
        try {
            redisTemplate.delete(key);
            log.info("Removed idempotency key {} for consumer group {}", eventId, consumerGroup);
        } catch (Exception e) {
            log.error("Failed to remove key from EventIdempotencyRegistry", e);
        }
    }
}
