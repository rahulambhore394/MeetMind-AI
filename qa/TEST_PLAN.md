# MeetMind AI - Master Test Plan

## 1. Scope & Objectives
The goal of this test plan is to validate all functional, non-functional, security, integration, and resilience capabilities of MeetMind AI across the Android client application and Spring Boot backend server.

---

## 2. Test Suites & Coverage Inventory

### 2.1 Backend Functional & API Test Scenarios
| Scenario ID | Endpoint / Feature | Preconditions | Input / Action | Expected Result | Execution Status |
|:---|:---|:---|:---|:---|:---:|
| **TC-AUTH-01** | `POST /api/auth/register` | None | Valid JSON payload with name, email, password | HTTP 201 Created, returns registered user details | **PASS** |
| **TC-AUTH-02** | `POST /api/auth/register` | Existing email | Registration with already registered email | HTTP 409 Conflict | **PASS** |
| **TC-AUTH-03** | `POST /api/auth/register` | None | Malformed payload (blank name or invalid email) | HTTP 400 Bad Request with validation errors | **PASS** |
| **TC-AUTH-04** | `POST /api/auth/login` | Registered user | Valid email and password | HTTP 200 OK with JWT accessToken and userId | **PASS** |
| **TC-AUTH-05** | `POST /api/auth/login` | Registered user | Incorrect password or blank fields | HTTP 400 Bad Request / 401 Unauthorized | **PASS** |
| **TC-MEET-01** | `POST /api/meetings` | Authenticated JWT | Valid title, description, scheduledAt | HTTP 201 Created with generated meeting code | **PASS** |
| **TC-MEET-02** | `POST /api/meetings` | Unauthenticated | Valid meeting payload without JWT | HTTP 403 Forbidden | **PASS** |
| **TC-MEET-03** | `POST /api/meetings` | Authenticated JWT | Blank title | HTTP 400 Bad Request | **PASS** |
| **TC-MEET-04** | `GET /api/meetings` | Authenticated JWT | Request all user meetings | HTTP 200 OK with list of meetings | **PASS** |
| **TC-MEET-05** | `GET /api/meetings/code/{code}` | Authenticated JWT | Lookup meeting by valid code | HTTP 200 OK with meeting details | **PASS** |
| **TC-MEET-06** | `DELETE /api/meetings/{id}` | Authenticated Host | Host deletes meeting by ID | HTTP 204 No Content | **PASS** |
| **TC-CHAT-01** | `GET /api/meetings/{id}/messages` | Authenticated participant | Retrieve paginated message history | HTTP 200 OK with PageImpl of messages | **PASS** |
| **TC-TRANS-01** | `POST /api/meetings/{id}/transcripts` | Authenticated participant | Save transcript segment | HTTP 200 OK, event emitted | **PASS** |
| **TC-WEBRTC-01**| `/topic/meetings/{id}/signaling` | STOMP connection | Send SDP Offer/Answer/ICE | Message broadcasted to appropriate peer | **PASS** |

### 2.2 Android Functional Test Scenarios
| Scenario ID | Feature / Component | Action | Expected Result | Execution Status |
|:---|:---|:---|:---|:---:|
| **TC-AND-01** | Gradle Compilation | `./gradlew assembleDebug` | APK compiled and packaged cleanly | **PASS** |
| **TC-AND-02** | Android Unit Test Suite | `./gradlew testDebugUnitTest` | Unit tests execute without failure | **PASS** |
| **TC-AND-03** | WebRTC Perfect Negotiation | Peer receives remote offer while in `HAVE_LOCAL_OFFER` | Polite peer rolls back cleanly; impolite ignores | **PASS** |
| **TC-AND-04** | Screen Sharing Renegotiation | User taps "Share Screen" | Track captured, offer generated, renegotiated | **PASS** |
| **TC-AND-05** | Track Disposal on Screen Share Stop | Stop screen sharing | Track stopped cleanly after renegotiation | **PASS** |
| **TC-AND-06** | ICE Restart Handling | Network handover / connection disruption | ICE restart triggered up to 3 times before terminal error | **PASS** |

### 2.3 Resilience & Security Scenarios
| Scenario ID | Scenario | Expected Behavior | Execution Status |
|:---|:---|:---|:---:|
| **TC-RES-01** | Kafka broker failure | System routes dead-letter messages to DLT without dropping requests | **PASS** |
| **TC-RES-02** | Expired JWT Token | Request rejected with HTTP 401 Unauthorized; prompt client token refresh | **PASS** |
| **TC-SEC-01** | SQL Injection in query params | Parameterized Spring Data repository eliminates SQL injection attack surface | **PASS** |
| **TC-SEC-02** | WebSocket Principal Cast | Both `User` and `UserPrincipal` handled cleanly without `ClassCastException` | **PASS** |
