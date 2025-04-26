
create table issuance_process (
   id varchar not null,
    ssi_name varchar,
    session_id varchar(255),
    successCBToken varchar,
    failCBToken varchar,
    successCBUri varchar(2048),
    failCBUri varchar(2048),
    ssiInvitationUrl varchar(2048),
    createdAt timestamp,
    primary key (id)
);


alter table requestsession
drop column successCBToken,
drop column failCBToken,
drop column successCBUri,
drop column failCBUri;

drop view notarizationrequest_view;

alter table notarizationrequest
drop column ssiInvitationUrl;

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
