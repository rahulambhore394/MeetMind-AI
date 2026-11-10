# Data Model

## Persisted entities

The schema is defined by Flyway V1-V6 migrations under `meetmind-backend/src/main/resources/db/migration`.

- `User` -> `users`: account identity and profile.
- `Meeting` -> `meetings`: host-owned meeting lifecycle, code, and status.
- `MeetingParticipant` -> `meeting_participants`: user-to-meeting membership and participation state.
- `ChatMessage` -> `chat_messages`: meeting-scoped messages authored by users.
- `MeetingRecording` -> `meeting_recordings`: meeting recording metadata and ownership.
- `RecordingChunk` -> `recording_chunks`: chunked recording storage metadata.
- `AudioMetadata` -> `audio_metadata`: one-to-one recording audio details.
- `MeetingTranscript` -> `transcripts`: meeting/recording transcript.
- `TranscriptSegment` -> `transcript_segments`: ordered transcript segments and optional speaker.
- `MeetingSummary` -> `meeting_summaries`: generated intelligence linked to meeting and optional transcript.
- `ActionItem` -> `meeting_action_items`: meeting follow-up items and status.
- `AiRepresentative` -> `ai_representatives`: meeting/user-owned AI representative configuration.
- `RepresentativeReport` -> `representative_reports`: AI representative output.
- `LiveTranslation` -> `live_translations`: translated transcript segment and speaker/language data.
- `Notification`, `UserDevice`, `NotificationPreference`: user notification state and device/preferences.

## Relationships

Meetings reference a required user host and own participants, chat messages, recordings, transcripts, summaries, action items, translations, representatives, and reports. Recordings own chunks and one audio metadata row; transcripts reference meetings and optionally recordings and own segments; summaries reference meetings and optionally transcripts and own action items. Users own meetings, participation rows, messages, recordings, representatives, reports, notifications, devices, and preferences.

## Transaction evidence

Transactional behavior is implemented in service classes and repository operations; exact scopes should be read from `@Transactional` annotations in the relevant service source. Database referential integrity and cascade behavior are explicitly evolved by V3, V5, and V6 migrations.
