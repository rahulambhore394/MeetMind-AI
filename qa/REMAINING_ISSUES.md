# MeetMind AI - Remaining Issues & Environmental Limitations

## 1. Documented Environmental Limitations

1. **Android Physical Device / Emulator Execution:**
   - **Status:** **BLOCKED / ENVIRONMENT LIMITATION**
   - **Reason:** No connected hardware device (`adb devices`) or running Android emulator instance was detected in the host environment.
   - **Mitigation:** Android compilation, code analysis, resource linking, and JVM unit tests (`testDebugUnitTest` and `assembleDebug`) were successfully executed. UI layouts and animations must undergo visual verification once booted on an emulator or physical handset.

2. **Google Gemini Live Audio & STT External Credentials:**
   - **Status:** **UNTESTED REAL CREDENTIALS (MOCK TESTED PASS)**
   - **Reason:** External Gemini API keys and cloud credentials are kept isolated from local test execution to comply with non-negotiable security rules.
   - **Mitigation:** The translation, AI representative, and transcription pipelines are verified through mocks and `TestRedisConfig` in the integration suite. Real-world end-to-end cloud latency should be observed upon staging deployment.

---

## 2. Low-Risk Advisory Items (Non-Blocking)

1. **Spring Mockito Deprecation Notices:**
   - Spring Boot 3.4.x marks `@MockBean` from `org.springframework.boot.test.mock.mockito` as deprecated in favor of `@MockitoBean` from Spring Test. Existing tests function correctly without issue, but gradual migration to `@MockitoBean` across older test classes is recommended.
2. **PostgreSQL Production Connection Pooling:**
   - Integration tests utilize H2 in-memory databases with HikariCP. In production, HikariCP max pool size and leak detection threshold should be tuned based on anticipated concurrent meeting concurrency.
