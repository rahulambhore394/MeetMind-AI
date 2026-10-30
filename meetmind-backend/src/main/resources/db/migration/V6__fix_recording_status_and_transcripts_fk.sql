-- V6: Fix recording status column length and foreign key from transcripts to meeting_recordings

-- 1. Increase status column length in meeting_recordings to accommodate longer enum values (e.g. READY_FOR_TRANSCRIPTION, TRANSCRIPTION_PROCESSING)
ALTER TABLE meeting_recordings ALTER COLUMN status TYPE VARCHAR(50);

-- 2. Clean up any invalid recording_id references in transcripts
UPDATE transcripts SET recording_id = NULL WHERE recording_id IS NOT NULL AND recording_id NOT IN (SELECT id FROM meeting_recordings);

-- 3. Fix foreign key on transcripts table to reference meeting_recordings(id)
ALTER TABLE transcripts DROP CONSTRAINT IF EXISTS transcripts_recording_id_fkey;
ALTER TABLE transcripts ADD CONSTRAINT transcripts_recording_id_fkey 
    FOREIGN KEY (recording_id) REFERENCES meeting_recordings(id) ON DELETE CASCADE;
