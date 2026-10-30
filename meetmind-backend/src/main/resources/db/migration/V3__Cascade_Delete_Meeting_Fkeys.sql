-- Add ON DELETE CASCADE to foreign key constraints referencing meetings and associated entities
-- 1. meeting_participants
DELETE FROM meeting_participants WHERE meeting_id NOT IN (SELECT id FROM meetings);
ALTER TABLE meeting_participants DROP CONSTRAINT IF EXISTS fk_participant_meeting;
ALTER TABLE meeting_participants DROP CONSTRAINT IF EXISTS meeting_participants_meeting_id_fkey;
ALTER TABLE meeting_participants ADD CONSTRAINT fk_participant_meeting 
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE;

-- 2. chat_messages
DELETE FROM chat_messages WHERE meeting_id NOT IN (SELECT id FROM meetings);
ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS chat_messages_meeting_id_fkey;
ALTER TABLE chat_messages ADD CONSTRAINT chat_messages_meeting_id_fkey 
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE;

-- 3. meeting_recordings
DELETE FROM meeting_recordings WHERE meeting_id NOT IN (SELECT id FROM meetings);
ALTER TABLE meeting_recordings DROP CONSTRAINT IF EXISTS meeting_recordings_meeting_id_fkey;
ALTER TABLE meeting_recordings ADD CONSTRAINT meeting_recordings_meeting_id_fkey 
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE;

-- 4. recording_chunks
DELETE FROM recording_chunks WHERE recording_id NOT IN (SELECT id FROM meeting_recordings);
ALTER TABLE recording_chunks DROP CONSTRAINT IF EXISTS recording_chunks_recording_id_fkey;
ALTER TABLE recording_chunks ADD CONSTRAINT recording_chunks_recording_id_fkey 
    FOREIGN KEY (recording_id) REFERENCES meeting_recordings(id) ON DELETE CASCADE;

-- 5. transcripts
DELETE FROM transcripts WHERE recording_id IS NOT NULL AND recording_id NOT IN (SELECT id FROM meeting_recordings);
ALTER TABLE transcripts DROP CONSTRAINT IF EXISTS transcripts_recording_id_fkey;
ALTER TABLE transcripts ADD CONSTRAINT transcripts_recording_id_fkey 
    FOREIGN KEY (recording_id) REFERENCES meeting_recordings(id) ON DELETE CASCADE;

DELETE FROM transcripts WHERE meeting_id NOT IN (SELECT id FROM meetings);
ALTER TABLE transcripts DROP CONSTRAINT IF EXISTS transcripts_meeting_id_fkey;
ALTER TABLE transcripts ADD CONSTRAINT transcripts_meeting_id_fkey 
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE;

-- 6. transcript_segments
DELETE FROM transcript_segments WHERE transcript_id NOT IN (SELECT id FROM transcripts);
ALTER TABLE transcript_segments DROP CONSTRAINT IF EXISTS transcript_segments_transcript_id_fkey;
ALTER TABLE transcript_segments ADD CONSTRAINT transcript_segments_transcript_id_fkey 
    FOREIGN KEY (transcript_id) REFERENCES transcripts(id) ON DELETE CASCADE;

-- 7. meeting_summaries
DELETE FROM meeting_summaries WHERE transcript_id IS NOT NULL AND transcript_id NOT IN (SELECT id FROM transcripts);
ALTER TABLE meeting_summaries DROP CONSTRAINT IF EXISTS meeting_summaries_transcript_id_fkey;
ALTER TABLE meeting_summaries ADD CONSTRAINT meeting_summaries_transcript_id_fkey 
    FOREIGN KEY (transcript_id) REFERENCES transcripts(id) ON DELETE CASCADE;

DELETE FROM meeting_summaries WHERE meeting_id NOT IN (SELECT id FROM meetings);
ALTER TABLE meeting_summaries DROP CONSTRAINT IF EXISTS meeting_summaries_meeting_id_fkey;
ALTER TABLE meeting_summaries ADD CONSTRAINT meeting_summaries_meeting_id_fkey 
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE;

-- 8. meeting_action_items
DELETE FROM meeting_action_items WHERE meeting_summary_id NOT IN (SELECT id FROM meeting_summaries);
ALTER TABLE meeting_action_items DROP CONSTRAINT IF EXISTS meeting_action_items_meeting_summary_id_fkey;
ALTER TABLE meeting_action_items ADD CONSTRAINT meeting_action_items_meeting_summary_id_fkey 
    FOREIGN KEY (meeting_summary_id) REFERENCES meeting_summaries(id) ON DELETE CASCADE;

DELETE FROM meeting_action_items WHERE meeting_id NOT IN (SELECT id FROM meetings);
ALTER TABLE meeting_action_items DROP CONSTRAINT IF EXISTS meeting_action_items_meeting_id_fkey;
ALTER TABLE meeting_action_items ADD CONSTRAINT meeting_action_items_meeting_id_fkey 
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE;

-- 9. ai_representatives
DELETE FROM ai_representatives WHERE meeting_id NOT IN (SELECT id FROM meetings);
ALTER TABLE ai_representatives DROP CONSTRAINT IF EXISTS ai_representatives_meeting_id_fkey;
ALTER TABLE ai_representatives ADD CONSTRAINT ai_representatives_meeting_id_fkey 
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE;

-- 10. representative_reports
DELETE FROM representative_reports WHERE representative_id NOT IN (SELECT id FROM ai_representatives);
ALTER TABLE representative_reports DROP CONSTRAINT IF EXISTS representative_reports_representative_id_fkey;
ALTER TABLE representative_reports ADD CONSTRAINT representative_reports_representative_id_fkey 
    FOREIGN KEY (representative_id) REFERENCES ai_representatives(id) ON DELETE CASCADE;

DELETE FROM representative_reports WHERE meeting_id NOT IN (SELECT id FROM meetings);
ALTER TABLE representative_reports DROP CONSTRAINT IF EXISTS representative_reports_meeting_id_fkey;
ALTER TABLE representative_reports ADD CONSTRAINT representative_reports_meeting_id_fkey 
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE;
