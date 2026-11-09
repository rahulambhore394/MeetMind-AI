package com.meetmind.meetmind_backend.webrtc;

import com.meetmind.meetmind_backend.config.WebRtcProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/webrtc")
public class WebRtcController {
    private final WebRtcProperties webRtcProperties;

    public WebRtcController(WebRtcProperties webRtcProperties) {
        this.webRtcProperties = webRtcProperties;
    }

    @GetMapping("/ice-servers")
    public ResponseEntity<List<WebRtcProperties.IceServer>> getIceServers() {
        List<WebRtcProperties.IceServer> servers = new ArrayList<>();
        if (webRtcProperties.getIceServers() != null) {
            webRtcProperties.getIceServers().stream()
                    .filter(Objects::nonNull)
                    .filter(server -> server.getUrls() != null && !server.getUrls().isEmpty())
                    .forEach(servers::add);
        }

        if (hasText(webRtcProperties.getTurnUrl())) {
            servers.add(new WebRtcProperties.IceServer(
                    List.of(webRtcProperties.getTurnUrl().trim()),
                    webRtcProperties.getTurnUsername(),
                    webRtcProperties.getTurnCredential()
            ));
        }
        return ResponseEntity.ok(servers);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
