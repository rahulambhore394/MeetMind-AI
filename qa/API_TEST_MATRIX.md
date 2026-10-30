# MeetMind AI - API Test Matrix

| Method | Endpoint Path | Auth Required | Input Schema | Expected Status | Verified Status | Test Suite Reference |
|:---:|:---|:---:|:---|:---:|:---:|:---|
| `POST` | `/api/auth/register` | No | `RegisterRequest` (name, email, password) | 201 Created / 400 Bad Request / 409 Conflict | **PASS** | `AuthControllerIntegrationTest` |
| `POST` | `/api/auth/login` | No | `LoginRequest` (email, password) | 200 OK / 400 Bad Request / 401 Unauthorized | **PASS** | `AuthControllerIntegrationTest` |
| `POST` | `/api/meetings` | Yes (JWT) | `CreateMeetingRequest` (title, description, scheduledAt) | 201 Created / 400 Bad Request / 403 Forbidden | **PASS** | `MeetingControllerIntegrationTest` |
| `GET` | `/api/meetings` | Yes (JWT) | None | 200 OK / 401 Unauthorized | **PASS** | `MeetingControllerIntegrationTest` |
| `GET` | `/api/meetings/code/{code}` | Yes (JWT) | Path param: code | 200 OK / 404 Not Found / 401 Unauthorized | **PASS** | `MeetingControllerIntegrationTest` |
| `DELETE`| `/api/meetings/{id}` | Yes (Host JWT) | Path param: id | 204 No Content / 403 Forbidden / 404 Not Found | **PASS** | `MeetingControllerIntegrationTest` |
| `GET` | `/api/meetings/{id}/messages` | Yes (JWT) | Path param: id, Query params: page, size | 200 OK / 403 Forbidden | **PASS** | `ChatControllerIntegrationTest` |
| `POST` | `/api/meetings/{id}/messages` | Yes (JWT) | Path param: id, `ChatMessageRequest` | 200 OK / 400 Bad Request / 403 Forbidden | **PASS** | `ChatControllerIntegrationTest` |
| `GET` | `/api/users/me` | Yes (JWT) | None | 200 OK / 401 Unauthorized | **PASS** | `UserController` (via `SecurityUtils`) |
| `GET` | `/api/meetings/{id}/participants` | Yes (JWT) | Path param: id | 200 OK / 403 Forbidden | **PASS** | `ParticipantController` (via `SecurityUtils`) |
| `GET` | `/api/meetings/{id}/transcripts` | Yes (JWT) | Path param: id | 200 OK | **PASS** | `TranscriptionIntegrationTest` |
| `POST` | `/api/meetings/{id}/representative/toggle` | Yes (Host JWT) | Path param: id, boolean state | 200 OK | **PASS** | `AiRepresentativeIntegrationTest` |
| `WS` | `/ws` | STOMP CONNECT | Header: `Authorization: Bearer <token>` | Connected / Connection Error | **PASS** | `SignalingIntegrationTest` |
| `TOPIC`| `/topic/meetings/{id}/signaling` | STOMP SUB/PUB | `SignalingMessage` (type, sdp, candidate, senderId) | Broadcast to peers | **PASS** | `SignalingIntegrationTest` |
| `TOPIC`| `/topic/meetings/{id}/translations` | STOMP SUB/PUB | `TranslationRequest` (text, targetLang) | Translated audio/text | **PASS** | `TranslationIntegrationTest` |
