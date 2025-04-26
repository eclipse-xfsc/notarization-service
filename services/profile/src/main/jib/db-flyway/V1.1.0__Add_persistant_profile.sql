create table persistent_profile (
    id varchar not null,
    profile_id varchar not null,
    issuanceContent JSON,
    capability varchar,
    kind varchar,
    name varchar,
    description varchar,
    encryption varchar,
    notaries varchar,
    valid_for varchar,
    is_revocable boolean,
    template JSON,
    documentTemplate varchar,
    taskDescriptions JSON,
    tasks JSON,
    pre_issuance_actions JSON,
    post_issuance_actions JSON,
    precondition_tasks JSON,
    action_descriptions JSON,
    updated_at TIMESTAMP,
    created_at TIMESTAMP not null,
    primary key (id)
);
alter table persistent_profile
add constraint UK_persistent_profile_profile_id UNIQUE(profile_id);
