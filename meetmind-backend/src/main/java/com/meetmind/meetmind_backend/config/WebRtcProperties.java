package com.meetmind.meetmind_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "webrtc")
public class WebRtcProperties {
    private List<IceServer> iceServers = new ArrayList<>();

    public List<IceServer> getIceServers() {
        return iceServers;
    }

    public void setIceServers(List<IceServer> iceServers) {
        this.iceServers = iceServers;
    }

    public static class IceServer {
        private List<String> urls;
        private String username;
        private String credential;

        public IceServer() {}

        public IceServer(List<String> urls, String username, String credential) {
            this.urls = urls;
            this.username = username;
            this.credential = credential;
        }

        public List<String> getUrls() {
            return urls;
        }

        public void setUrls(List<String> urls) {
            this.urls = urls;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getCredential() {
            return credential;
        }

        public void setCredential(String credential) {
            this.credential = credential;
        }
    }
}
