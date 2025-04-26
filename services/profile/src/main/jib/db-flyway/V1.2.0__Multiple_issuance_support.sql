
alter table ProfileDid
add column issuance_version varchar;

alter table ProfileDid
drop constraint UK_ProfileDid_issuingDid;

CREATE INDEX IDX_profiledid_profileid ON ProfileDid (profileId);
