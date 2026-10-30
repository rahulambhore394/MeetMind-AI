# MeetMind AI - Bug Tracker

| Bug ID | Component | Severity | Description & Root Cause | Fix Applied | Verification Status |
|:---|:---|:---:|:---|:---|:---:|
| **BUG-SEC-01** | Backend Controller / Security | **P1 - High** | `ClassCastException` and missing user mapping in WebSocket controllers (`TranslationWebSocketController`, `MeetingController`, etc.) due to mismatch between `User` and `UserPrincipal`. | Implemented `SecurityUtils.extractUser()` supporting `User`, `UserPrincipal`, and `UserDetails`. | **VERIFIED (PASS)** |
| **BUG-TEST-02**| Backend Test Automation | **P2 - Medium**| Lack of MockMvc integration tests for authentication and meeting creation, leading to untested security filters. | Created `AuthControllerIntegrationTest` and `MeetingControllerIntegrationTest`. | **VERIFIED (PASS)** |
| **BUG-RTC-01** | Android WebRTC | **P1 - High** | WebRTC SDP Glare / Offer Collision when two peers join concurrently. | Implemented Perfect Negotiation Pattern with polite/impolite peer designation based on IDs. | **VERIFIED (PASS)** |
| **BUG-RTC-02** | Android WebRTC | **P1 - High** | Screen Sharing track added without renegotiation, causing remote peers not to render new screen track. | Implemented renegotiation offer/answer flow in `startScreenSharing()`. | **VERIFIED (PASS)** |
| **BUG-RTC-03** | Android WebRTC | **P1 - High** | Asynchronous Track Disposal race condition when stopping screen share. | Ensured local and peer connection track disposal happens safely after signaling completion. | **VERIFIED (PASS)** |
| **BUG-RTC-04** | Android WebRTC | **P2 - Medium**| Zero-ID politeness deadlock when uninitialized IDs (0L) evaluated both peers as polite. | Added non-zero ID validation and tie-breaker comparison. | **VERIFIED (PASS)** |
| **BUG-RTC-05** | Android WebRTC | **P2 - Medium**| ICE restart silent exhaustion without notifying UI of terminal connection failure. | Added UI failure callback trigger upon exceeding 3 retry attempts. | **VERIFIED (PASS)** |
| **BUG-RTC-06** | Android WebRTC | **P2 - Medium**| Native WebRTC empty ROLLBACK crash risk on Android. | Fixed rollback description to use valid empty SDP structure instead of malformed string. | **VERIFIED (PASS)** |
