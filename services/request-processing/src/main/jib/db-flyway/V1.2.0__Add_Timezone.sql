ALTER TABLE Document ALTER COLUMN createdAt TYPE TIMESTAMP WITH TIME ZONE USING createdAt AT TIME ZONE 'UTC';
ALTER TABLE Document ALTER COLUMN lastModified TYPE TIMESTAMP WITH TIME ZONE USING lastModified AT TIME ZONE 'UTC';

ALTER TABLE DocumentStoreDocument ALTER COLUMN createdAt TYPE TIMESTAMP WITH TIME ZONE USING createdAt AT TIME ZONE 'UTC';
ALTER TABLE DocumentStoreDocument ALTER COLUMN lastModified TYPE TIMESTAMP WITH TIME ZONE USING lastModified AT TIME ZONE 'UTC';

ALTER TABLE HttpNotarizationRequestAudit ALTER COLUMN createdAt TYPE TIMESTAMP WITH TIME ZONE USING createdAt AT TIME ZONE 'UTC';
ALTER TABLE HttpNotarizationRequestAudit ALTER COLUMN receivedAt TYPE TIMESTAMP WITH TIME ZONE USING receivedAt AT TIME ZONE 'UTC';

DROP VIEW notarizationrequest_view;
ALTER TABLE notarizationrequest ALTER COLUMN createdAt TYPE TIMESTAMP WITH TIME ZONE USING createdAt AT TIME ZONE 'UTC';
ALTER TABLE notarizationrequest ALTER COLUMN lastModified TYPE TIMESTAMP WITH TIME ZONE USING lastModified AT TIME ZONE 'UTC';
create or replace view notarizationrequest_view as
    select notarizationrequest.*,
        notarizationrequest.id as request_id,
        reqsess.state,
        reqsess.profileId,
        docs.total as total_documents
    from notarizationrequest
    left join requestsession as reqsess on reqsess.id = notarizationrequest.session_id
    left join (
        select session_id, count(*) as total
        from document
        group by session_id
    ) docs on reqsess.id = docs.session_id
;

ALTER TABLE requestsession ALTER COLUMN createdAt TYPE TIMESTAMP WITH TIME ZONE USING createdAt AT TIME ZONE 'UTC';
ALTER TABLE requestsession ALTER COLUMN lastModified TYPE TIMESTAMP WITH TIME ZONE USING lastModified AT TIME ZONE 'UTC';
ALTER TABLE requestsession ALTER COLUMN version TYPE TIMESTAMP WITH TIME ZONE USING version AT TIME ZONE 'UTC';

ALTER TABLE requestor_identity ALTER COLUMN createdAt TYPE TIMESTAMP WITH TIME ZONE USING createdAt AT TIME ZONE 'UTC';

ALTER TABLE session_task ALTER COLUMN createdAt TYPE TIMESTAMP WITH TIME ZONE USING createdAt AT TIME ZONE 'UTC';
