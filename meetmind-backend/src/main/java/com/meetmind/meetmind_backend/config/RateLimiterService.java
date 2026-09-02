package com.meetmind.meetmind_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);
    private final StringRedisTemplate redisTemplate;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String limitKey, String operation, int maxRequests, int windowSeconds) {
        try {
            String key = "ratelimit:" + limitKey + ":" + operation;
            long nowEpoch = Instant.now().getEpochSecond();
            long windowStart = nowEpoch - windowSeconds;

            // Remove old entries outside the sliding window
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

            // Count the requests within the window
            Long currentRequests = redisTemplate.opsForZSet().zCard(key);
            if (currentRequests != null && currentRequests >= maxRequests) {
                return false;
            }

            // Record current request with a unique member to handle concurrent hits
            String member = nowEpoch + ":" + System.nanoTime();
            redisTemplate.opsForZSet().add(key, member, nowEpoch);

            // Set TTL on the set key so it self-cleans
            redisTemplate.expire(key, windowSeconds * 2, java.util.concurrent.TimeUnit.SECONDS);

            return true;
        } catch (Exception e) {
            log.warn("Failed to check rate limit for key {} in Redis: {}. Failing open.", limitKey, e.getMessage());
            return true; // Fail-open on Redis outage
        }
    }
}
