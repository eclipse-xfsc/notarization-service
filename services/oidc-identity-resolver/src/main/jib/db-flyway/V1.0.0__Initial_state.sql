
    create table Session (
        id uuid not null,
        cancelNonce varchar(512),
        failURI bytea,
        loginNonce varchar(512),
        successURI bytea,
        primary key (id)
    );
