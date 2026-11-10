package com.meetmind.meetmind_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "livekit")
public class LiveKitProperties {

    /**
     * Public WebSocket RTC URL for clients to connect to the LiveKit SFU.
     * e.g. "ws://10.35.242.113:7880" (local) or "wss://rtc.meetmind.ai" (prod)
     */
    private String url = "ws://127.0.0.1:7880";

    /**
     * LiveKit API Key used to issue tokens.
     */
    private String apiKey = "devkey";

    /**
     * LiveKit API Secret used to sign JWT access tokens.
     */
    private String apiSecret = "secret";

    /**
     * Token TTL in seconds (default 3600 = 1 hour).
     */
    private long tokenTtlSeconds = 3600;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public long getTokenTtlSeconds() {
        return tokenTtlSeconds;
    }

    public void setTokenTtlSeconds(long tokenTtlSeconds) {
        this.tokenTtlSeconds = tokenTtlSeconds;
    }
}

