package com.meetmind.meetmind_backend.chat;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

@TestConfiguration
@EnableCaching
public class TestRedisConfig {

    private final Map<String, String> valueMap = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> setMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Double>> zsetMap = new ConcurrentHashMap<>();
    private final Map<String, Long> ttlMap = new ConcurrentHashMap<>();

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public org.springframework.cache.CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("meetings");
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);

        // ValueOperations Mock
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);

        Mockito.doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            valueMap.put(key, value);
            return null;
        }).when(valueOps).set(anyString(), anyString());

        Mockito.doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            valueMap.put(key, value);
            return null;
        }).when(valueOps).set(anyString(), anyString(), anyLong(), any());

        when(valueOps.setIfAbsent(anyString(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            if (valueMap.containsKey(key)) {
                return false;
            }
            valueMap.put(key, value);
            return true;
        });

        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            if (valueMap.containsKey(key)) {
                return false;
            }
            valueMap.put(key, value);
            return true;
        });

        when(valueOps.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return valueMap.get(key);
        });

        // SetOperations Mock
        SetOperations<String, String> setOps = mock(SetOperations.class);
        when(template.opsForSet()).thenReturn(setOps);

        when(setOps.add(anyString(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object valArg = invocation.getArgument(1);
            Set<String> set = setMap.computeIfAbsent(key, k -> new HashSet<>());
            if (valArg instanceof Object[] vals) {
                long added = 0;
                for (Object v : vals) {
                    if (set.add(String.valueOf(v))) {
                        added++;
                    }
                }
                return added;
            } else {
                return set.add(String.valueOf(valArg)) ? 1L : 0L;
            }
        });

        when(setOps.remove(anyString(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object valArg = invocation.getArgument(1);
            Set<String> set = setMap.get(key);
            if (set == null) return 0L;
            if (valArg instanceof Object[] vals) {
                long removed = 0;
                for (Object v : vals) {
                    if (set.remove(String.valueOf(v))) {
                        removed++;
                    }
                }
                return removed;
            } else {
                return set.remove(String.valueOf(valArg)) ? 1L : 0L;
            }
        });

        when(setOps.size(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Set<String> set = setMap.get(key);
            return set == null ? 0L : (long) set.size();
        });

        when(setOps.members(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Set<String> set = setMap.get(key);
            return set == null ? Collections.emptySet() : set;
        });

        // ZSetOperations Mock
        ZSetOperations<String, String> zsetOps = mock(ZSetOperations.class);
        when(template.opsForZSet()).thenReturn(zsetOps);

        when(zsetOps.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            double min = invocation.getArgument(1);
            double max = invocation.getArgument(2);
            Map<String, Double> map = zsetMap.get(key);
            if (map == null) return 0L;
            long removed = 0;
            Iterator<Map.Entry<String, Double>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Double> entry = it.next();
                if (entry.getValue() >= min && entry.getValue() <= max) {
                    it.remove();
                    removed++;
                }
            }
            return removed;
        });

        when(zsetOps.zCard(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Map<String, Double> map = zsetMap.get(key);
            return map == null ? 0L : (long) map.size();
        });

        when(zsetOps.add(anyString(), anyString(), anyDouble())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            double score = invocation.getArgument(2);
            Map<String, Double> map = zsetMap.computeIfAbsent(key, k -> new HashMap<>());
            map.put(value, score);
            return true;
        });

        // Template basic methods Mock
        when(template.expire(anyString(), anyLong(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            long seconds = invocation.getArgument(1);
            ttlMap.put(key, seconds);
            return true;
        });

        when(template.hasKey(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return valueMap.containsKey(key) || setMap.containsKey(key) || zsetMap.containsKey(key);
        });

        when(template.delete(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            boolean existed = valueMap.remove(key) != null || setMap.remove(key) != null || zsetMap.remove(key) != null;
            ttlMap.remove(key);
            return existed;
        });

        return template;
    }
}
