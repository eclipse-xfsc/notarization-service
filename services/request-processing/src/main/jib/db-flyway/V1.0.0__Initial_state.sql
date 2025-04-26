
    create table Document (
       id uuid not null,
        content bytea,
        createdAt timestamp,
        hash varchar(255),
        lastModified timestamp,
        longDescription bytea,
        shortDescription bytea,
        title bytea,
        extension bytea,
        mimetype bytea,
        verificationReport bytea,
        session_id varchar(255),
        primary key (id)
    );

    create table DocumentStoreDocument (
       id uuid not null,
        content bytea,
        createdAt timestamp,
        hash varchar(255),
        lastModified timestamp,
        longDescription bytea,
        shortDescription bytea,
        extension bytea,
        mimetype bytea,
        taskId uuid,
        title bytea,
        verificationReport bytea,
        primary key (id)
    );

    create table HttpNotarizationRequestAudit (
       id uuid not null,
        action varchar(50),
        caller varchar(255),
        createdAt timestamp,
        httpStatus int4 not null,
        ipAddress varchar(255),
        notarizationId varchar(255),
        receivedAt timestamp,
        requestContent bytea,
        requestUri varchar(500),
        sessionId varchar(255),
        taskName varchar(128),
        primary key (id)
    );

    create table notarizationrequest (
       id uuid not null,
        claimedBy varchar(255),
        createdAt timestamp,
        data bytea,
        did varchar(255),
        lastModified timestamp,
        rejectComment varchar(255),
        session_id varchar(255),
        requestorInvitationUrl varchar(2048),
        ssiInvitationUrl varchar(2048),
        primary key (id)
    );

    create table OngoingTask (
       taskId uuid not null,
        nonce varchar(255),
        cancelUri bytea,
        primary key (taskId)
    );

    create table requestsession (
       id varchar(255) not null,
        accessToken varchar(255) unique,
        createdAt timestamp,
        identityToken varchar(255) unique,
        lastModified timestamp,
        profileId varchar(255),
        state int4,
        manualRelease boolean,
        manualReleaseToken varchar(255) unique,
        version timestamp,
        successCBToken varchar(255) unique,
        failCBToken varchar(255) unique,
        successCBUri varchar(500),
        failCBUri varchar(500),
        primary key (id)
    );

    create table requestor_identity (
       id uuid not null,
        algorithm bytea,
        createdAt timestamp,
        data bytea,
        encryption bytea,
        jwk bytea,
        session_id varchar(255),
        primary key (id)
    );

    create table session_task (
       taskId uuid not null,
        createdAt timestamp,
        fulfilled boolean not null,
        name varchar(255),
        running boolean not null,
        type int4,
        session_id varchar(255),
        primary key (taskId)
    );

    create table ongoingdocumenttask (
        taskId uuid not null,
        primary key (taskId)
    );

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

    alter table if exists notarizationrequest 
       add constraint UK_g0gnr9xrln8tsfo3xengp52h2 unique (session_id);

    alter table if exists Document 
       add constraint FKiwe6mc0vjgeqhhsya0vrui1df 
       foreign key (session_id)
       references requestsession
       ON DELETE CASCADE;

    alter table if exists notarizationrequest 
       add constraint FKndchmxjn0yo483aco0b00seyi 
       foreign key (session_id) 
       references requestsession
       ON DELETE CASCADE;

    alter table if exists requestor_identity 
       add constraint FKechj6pw55vcsyul1owey1ft9s 
       foreign key (session_id) 
       references requestsession
       ON DELETE CASCADE;

    alter table if exists session_task 
       add constraint FKkhkcsxik1q067dyc80sy66uge 
       foreign key (session_id) 
       references requestsession
       ON DELETE CASCADE;
