
ALTER TABLE requestor_identity
ADD COLUMN taskid UUID,
ADD COLUMN worktype varchar,
Add COLUMN successful boolean;

ALTER TABLE session_task
ADD COLUMN worktype varchar;
