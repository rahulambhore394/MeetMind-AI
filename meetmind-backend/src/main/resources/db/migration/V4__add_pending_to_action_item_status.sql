ALTER TABLE meeting_action_items DROP CONSTRAINT IF EXISTS meeting_action_items_status_check;

ALTER TABLE meeting_action_items ADD CONSTRAINT meeting_action_items_status_check 
    CHECK (status::text = ANY (ARRAY['PENDING'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'OPEN'::character varying, 'NEEDS_REVIEW'::character varying]::text[]));
