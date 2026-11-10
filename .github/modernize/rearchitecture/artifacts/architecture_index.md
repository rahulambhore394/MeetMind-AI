# Architecture Index

This index is not the full contract. Do not implement from this file alone; follow the artifact paths below.

## Implementation Guide

### Global artifacts

- `unit_graph.yaml`: all workers use this for entrypoints, signatures, dependencies, dynamic bindings, and shared reference whitelists.
- `migration_boundary.yaml`: analysis-only scope; a future implementation must use `must_rewrite`, not `source_anchors`.
- `wire_contracts.yaml`: filter by `unit`; preserve frozen REST, STOMP, and Kafka contracts.
- `shared_modules.yaml`: filter `used_by_units`; only use fields listed by the unit's `shared_refs`.
- `cross_unit_state.yaml`: filter flows where the unit is writer or reader; runtime-confirm rows marked `must_confirm: runtime`.
- `project-structure.md`, `tech-stack.md`, `data-model.md`: global source and runtime context.

### Per-unit contract

Every unit has `units/<unit>/behavior.yaml`, `bindings.yaml`, and `unit_decomposition.yaml`. Read all three before implementation. Behavior records must-preserve side effects, branches, errors, and concurrency; bindings records framework wiring and runtime configuration; decomposition is advisory only and always has `commit: false`.

### Units and completion evidence

The unit list and external triggers are authoritative in `unit_graph.yaml`. Before DONE, an implementation worker must report: artifact paths read; every implemented `must_preserve` and `must_appear_in_target` item; unresolved/deferred contracts; and build, test, or runtime evidence.

### Unit artifact paths

For each assigned trigger below, read the three exact paths shown, then filter the global artifacts by the same unit name:

- `auth-api` (POST auth): `units/auth-api/behavior.yaml`, `units/auth-api/bindings.yaml`, `units/auth-api/unit_decomposition.yaml`
- `meetings-api` (meeting CRUD/lifecycle): `units/meetings-api/behavior.yaml`, `units/meetings-api/bindings.yaml`, `units/meetings-api/unit_decomposition.yaml`
- `participant-api` (membership): `units/participant-api/behavior.yaml`, `units/participant-api/bindings.yaml`, `units/participant-api/unit_decomposition.yaml`
- `recording-api` (recording transfer): `units/recording-api/behavior.yaml`, `units/recording-api/bindings.yaml`, `units/recording-api/unit_decomposition.yaml`
- `transcript-api` (transcript HTTP): `units/transcript-api/behavior.yaml`, `units/transcript-api/bindings.yaml`, `units/transcript-api/unit_decomposition.yaml`
- `intelligence-api` (reports/generation): `units/intelligence-api/behavior.yaml`, `units/intelligence-api/bindings.yaml`, `units/intelligence-api/unit_decomposition.yaml`
- `translation-api` (translation HTTP): `units/translation-api/behavior.yaml`, `units/translation-api/bindings.yaml`, `units/translation-api/unit_decomposition.yaml`
- `notification-api` (notifications/devices): `units/notification-api/behavior.yaml`, `units/notification-api/bindings.yaml`, `units/notification-api/unit_decomposition.yaml`
- `user-api` (current user): `units/user-api/behavior.yaml`, `units/user-api/bindings.yaml`, `units/user-api/unit_decomposition.yaml`
- `chat-api` (chat HTTP): `units/chat-api/behavior.yaml`, `units/chat-api/bindings.yaml`, `units/chat-api/unit_decomposition.yaml`
- `webrtc-api` (ICE servers): `units/webrtc-api/behavior.yaml`, `units/webrtc-api/bindings.yaml`, `units/webrtc-api/unit_decomposition.yaml`
- `chat-websocket` (STOMP chat): `units/chat-websocket/behavior.yaml`, `units/chat-websocket/bindings.yaml`, `units/chat-websocket/unit_decomposition.yaml`
- `signaling-websocket` (STOMP WebRTC): `units/signaling-websocket/behavior.yaml`, `units/signaling-websocket/bindings.yaml`, `units/signaling-websocket/unit_decomposition.yaml`
- `translation-websocket` (STOMP translation): `units/translation-websocket/behavior.yaml`, `units/translation-websocket/bindings.yaml`, `units/translation-websocket/unit_decomposition.yaml`
- `ai-proxy-websocket` (STOMP AI proxy): `units/ai-proxy-websocket/behavior.yaml`, `units/ai-proxy-websocket/bindings.yaml`, `units/ai-proxy-websocket/unit_decomposition.yaml`
- `transcription-consumer` (Kafka transcription): `units/transcription-consumer/behavior.yaml`, `units/transcription-consumer/bindings.yaml`, `units/transcription-consumer/unit_decomposition.yaml`
- `intelligence-consumer` (Kafka intelligence): `units/intelligence-consumer/behavior.yaml`, `units/intelligence-consumer/bindings.yaml`, `units/intelligence-consumer/unit_decomposition.yaml`
- `recording-consumer` (Kafka recording): `units/recording-consumer/behavior.yaml`, `units/recording-consumer/bindings.yaml`, `units/recording-consumer/unit_decomposition.yaml`
- `event-consumers` (Kafka notification/analytics/audit/DLT): `units/event-consumers/behavior.yaml`, `units/event-consumers/bindings.yaml`, `units/event-consumers/unit_decomposition.yaml`
- `reminder-scheduler` (60-second job): `units/reminder-scheduler/behavior.yaml`, `units/reminder-scheduler/bindings.yaml`, `units/reminder-scheduler/unit_decomposition.yaml`
- `android-client` (launcher/navigation/services): `units/android-client/behavior.yaml`, `units/android-client/bindings.yaml`, `units/android-client/unit_decomposition.yaml`

## Completeness

The artifact set covers the Spring Boot HTTP/STOMP/Kafka/scheduled surfaces and the Android launcher/runtime services. Generated build outputs are not source anchors.
