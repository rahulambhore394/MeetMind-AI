package com.meetmind.meetmind_backend.webrtc;

import com.meetmind.meetmind_backend.config.WebRtcProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/webrtc")
public class WebRtcController {
    private final WebRtcProperties webRtcProperties;

    public WebRtcController(WebRtcProperties webRtcProperties) {
        this.webRtcProperties = webRtcProperties;
    }

    @GetMapping("/ice-servers")
    public ResponseEntity<List<WebRtcProperties.IceServer>> getIceServers() {
        return ResponseEntity.ok(webRtcProperties.getIceServers());
    }
}
