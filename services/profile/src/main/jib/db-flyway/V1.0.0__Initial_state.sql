create table ProfileDid (
    id uuid not null,
    profileId varchar not null,
    issuanceContent JSON,
    primary key (id)
);
alter table ProfileDid
add constraint UK_ProfileDid_issuingDid UNIQUE(profileId);
