-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Meetings table
CREATE TABLE IF NOT EXISTS meetings (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    scheduled_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    host_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Participants table
CREATE TABLE IF NOT EXISTS meeting_participants (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    joined_at TIMESTAMP,
    left_at TIMESTAMP,
    UNIQUE(meeting_id, user_id)
);

-- Chat Messages
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    message TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Recordings
CREATE TABLE IF NOT EXISTS meeting_recordings (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id),
    owner_id BIGINT NOT NULL REFERENCES users(id),
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    duration BIGINT,
    storage_path VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    media_format VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Recording Chunks
CREATE TABLE IF NOT EXISTS recording_chunks (
    id BIGSERIAL PRIMARY KEY,
    recording_id BIGINT NOT NULL REFERENCES meeting_recordings(id),
    chunk_index INTEGER NOT NULL,
    start_ms BIGINT NOT NULL,
    end_ms BIGINT NOT NULL,
    storage_path VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Audio Metadata
CREATE TABLE IF NOT EXISTS audio_metadata (
    recording_id BIGINT PRIMARY KEY REFERENCES meeting_recordings(id),
    codec VARCHAR(50),
    sample_rate INTEGER,
    channels INTEGER,
    bit_rate BIGINT,
    duration_ms BIGINT,
    format VARCHAR(20)
);

-- Transcripts
CREATE TABLE IF NOT EXISTS transcripts (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id),
    recording_id BIGINT REFERENCES meeting_recordings(id),
    full_text TEXT,
    status VARCHAR(30) NOT NULL,
    language VARCHAR(10),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Transcript Segments
CREATE TABLE IF NOT EXISTS transcript_segments (
    id BIGSERIAL PRIMARY KEY,
    transcript_id BIGINT NOT NULL REFERENCES transcripts(id),
    user_id BIGINT REFERENCES users(id),
    text TEXT NOT NULL,
    start_time DOUBLE PRECISION,
    end_time DOUBLE PRECISION,
    segment_index INTEGER NOT NULL
);

-- Meeting Summaries
CREATE TABLE IF NOT EXISTS meeting_summaries (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id),
    transcript_id BIGINT REFERENCES transcripts(id),
    summary TEXT,
    key_points_json TEXT,
    decisions_json TEXT,
    topics_json TEXT,
    questions_json TEXT,
    analysis_json TEXT,
    status VARCHAR(30) NOT NULL,
    provider_name VARCHAR(100),
    model_metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Action Items
CREATE TABLE IF NOT EXISTS meeting_action_items (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id),
    meeting_summary_id BIGINT REFERENCES meeting_summaries(id),
    description TEXT NOT NULL,
    assigned_user VARCHAR(255),
    due_date VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    confidence DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- AI Representatives
CREATE TABLE IF NOT EXISTS ai_representatives (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id),
    meeting_id BIGINT NOT NULL REFERENCES meetings(id),
    status VARCHAR(30) NOT NULL,
    media_storage_path VARCHAR(500),
    monitored_topics_json TEXT,
    monitored_questions_json TEXT,
    important_people_json TEXT,
    report_preferences_json TEXT,
    notification_preferences_json TEXT,
    consent_disclosure BOOLEAN NOT NULL DEFAULT TRUE,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Representative Reports
CREATE TABLE IF NOT EXISTS representative_reports (
    id BIGSERIAL PRIMARY KEY,
    representative_id BIGINT NOT NULL REFERENCES ai_representatives(id),
    owner_id BIGINT NOT NULL REFERENCES users(id),
    meeting_id BIGINT NOT NULL REFERENCES meetings(id),
    summary TEXT,
    important_discussions_json TEXT,
    decisions_json TEXT,
    action_items_json TEXT,
    owner_relevant_questions_json TEXT,
    monitored_topics_found_json TEXT,
    transcript_references_json TEXT,
    attendance_timeline_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    related_meeting_id BIGINT,
    related_representative_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP
);

-- User Devices
CREATE TABLE IF NOT EXISTS user_devices (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    fcm_token TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Notification Preferences
CREATE TABLE IF NOT EXISTS notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    meeting_reminders BOOLEAN NOT NULL DEFAULT TRUE,
    meeting_started BOOLEAN NOT NULL DEFAULT TRUE,
    chat_messages BOOLEAN NOT NULL DEFAULT TRUE,
    ai_insights BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE
);

-- Live Translations
CREATE TABLE IF NOT EXISTS live_translations (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id),
    transcript_segment_id BIGINT REFERENCES transcript_segments(id),
    original_text TEXT NOT NULL,
    translated_text TEXT NOT NULL,
    source_language VARCHAR(10) NOT NULL,
    target_language VARCHAR(10) NOT NULL,
    speaker_id BIGINT REFERENCES users(id),
    speaker_name VARCHAR(100),
    is_final BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_meetings_host_id ON meetings(host_id);
CREATE INDEX IF NOT EXISTS idx_participants_meeting_id ON meeting_participants(meeting_id);
CREATE INDEX IF NOT EXISTS idx_participants_user_id ON meeting_participants(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_meeting_id ON chat_messages(meeting_id);
CREATE INDEX IF NOT EXISTS idx_recordings_meeting_id ON meeting_recordings(meeting_id);
CREATE INDEX IF NOT EXISTS idx_recording_chunks_recording_id ON recording_chunks(recording_id);
CREATE INDEX IF NOT EXISTS idx_transcripts_meeting_id ON transcripts(meeting_id);
CREATE INDEX IF NOT EXISTS idx_segments_transcript_id ON transcript_segments(transcript_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_user_devices_user_id ON user_devices(user_id);
CREATE INDEX IF NOT EXISTS idx_lt_meeting_target_lang ON live_translations(meeting_id, target_language);
CREATE INDEX IF NOT EXISTS idx_lt_segment_target_lang ON live_translations(transcript_segment_id, target_language);
