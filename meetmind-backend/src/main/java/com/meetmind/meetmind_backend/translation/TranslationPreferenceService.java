package com.meetmind.meetmind_backend.translation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class TranslationPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(TranslationPreferenceService.class);

    private final StringRedisTemplate redisTemplate;
    // In-memory fallback for local/test environments when Redis is unavailable
    private final Map<String, String> localUserPreferences = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> localMeetingTargetLangs = new ConcurrentHashMap<>();

    public TranslationPreferenceService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Stores a participant's preferred translation language for a specific meeting.
     */
    public void setUserLanguagePreference(Long meetingId, Long userId, String targetLanguage) {
        if (meetingId == null || userId == null || targetLanguage == null) return;
        String lang = targetLanguage.trim().toLowerCase();

        // 1. Try Redis
        if (redisTemplate != null) {
            try {
                String userKey = "meeting:" + meetingId + ":user:" + userId + ":target_lang";
                String meetingLangsKey = "meeting:" + meetingId + ":target_langs";
                redisTemplate.opsForValue().set(userKey, lang, 24, TimeUnit.HOURS);
                redisTemplate.opsForSet().add(meetingLangsKey, lang);
                redisTemplate.expire(meetingLangsKey, 24, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Redis error setting translation preference for user {} in meeting {}: {}",
                        userId, meetingId, e.getMessage());
            }
        }

        // 2. In-memory backup
        localUserPreferences.put(meetingId + ":" + userId, lang);
        localMeetingTargetLangs.computeIfAbsent(meetingId, k -> ConcurrentHashMap.newKeySet()).add(lang);
    }

    /**
     * Retrieves the preferred language for a user in a meeting (or default "en").
     */
    public String getUserLanguagePreference(Long meetingId, Long userId) {
        if (meetingId == null || userId == null) return "en";

        if (redisTemplate != null) {
            try {
                String userKey = "meeting:" + meetingId + ":user:" + userId + ":target_lang";
                String lang = redisTemplate.opsForValue().get(userKey);
                if (lang != null && !lang.isBlank()) return lang;
            } catch (Exception e) {
                log.warn("Redis error getting translation preference for user {} in meeting {}: {}",
                        userId, meetingId, e.getMessage());
            }
        }

        return localUserPreferences.getOrDefault(meetingId + ":" + userId, "en");
    }

    /**
     * Retrieves all unique target languages actively requested by participants in this meeting.
     */
    public Set<String> getActiveTargetLanguages(Long meetingId) {
        if (meetingId == null) return Collections.emptySet();
        Set<String> result = new HashSet<>();

        if (redisTemplate != null) {
            try {
                String meetingLangsKey = "meeting:" + meetingId + ":target_langs";
                Set<String> members = redisTemplate.opsForSet().members(meetingLangsKey);
                if (members != null && !members.isEmpty()) {
                    result.addAll(members);
                }
            } catch (Exception e) {
                log.warn("Redis error reading target languages for meeting {}: {}", meetingId, e.getMessage());
            }
        }

        Set<String> local = localMeetingTargetLangs.get(meetingId);
        if (local != null) {
            result.addAll(local);
        }

        return result;
    }

    /**
     * Clears preferences when a meeting ends.
     */
    public void clearMeetingPreferences(Long meetingId) {
        if (meetingId == null) return;
        if (redisTemplate != null) {
            try {
                String meetingLangsKey = "meeting:" + meetingId + ":target_langs";
                redisTemplate.delete(meetingLangsKey);
            } catch (Exception ignored) {}
        }
        localMeetingTargetLangs.remove(meetingId);
    }
}
