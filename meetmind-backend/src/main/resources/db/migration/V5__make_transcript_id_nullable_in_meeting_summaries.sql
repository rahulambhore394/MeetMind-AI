-- Allow meeting_summaries to be saved even when a meeting concluded without spoken transcript
ALTER TABLE meeting_summaries ALTER COLUMN transcript_id DROP NOT NULL;
