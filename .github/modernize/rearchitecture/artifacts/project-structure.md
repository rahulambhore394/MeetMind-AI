# Project Structure

## Classification

Full-stack monorepo with one Spring Boot backend and one Android client. Generated directories (`target`, `build`, `.gradle`, APK/JAR outputs) are excluded from source analysis.

## Modules and layers

- `meetmind-backend/`: Java 21 Spring Boot service.
  - `auth`, `user`: identity, JWT security, profile.
  - `meeting`, `participant`: meeting lifecycle and membership.
  - `recording`, `transcription`, `translation`: media and language workflows.
  - `intelligence`, `representative`: AI reports, action items, AI representatives.
  - `chat`, `notification`, `webrtc`, `websocket`: realtime and user communication.
  - `event`, `config`, `common`: Kafka/eventing, infrastructure configuration, cross-cutting errors/security.
  - Persistence is JPA repositories and Flyway migrations under `src/main/resources/db/migration`.
- `android/app/`: Kotlin/Jetpack Compose client.
  - `feature/*`: auth, home, meetings, participants, recording, transcript, intelligence, translation, chat, notifications, profile, representatives, settings, onboarding.
  - `core/*`: navigation, DI, networking, FCM, WebRTC, media recording, design system.

## Entry points

- Backend: `MeetmindBackendApplication.main`, REST controllers, STOMP `@MessageMapping` handlers, Kafka `@KafkaListener` consumers, and `ReminderScheduler`.
- Android: manifest launcher `MainActivity`, Compose navigation graph, FCM service, and foreground recording service.

## Functional domains

Authentication/profile; meeting scheduling and lifecycle; participant presence; recordings and transcription; live translation; AI intelligence and action items; AI representatives; chat and notifications; WebRTC signaling; analytics/audit event processing.
