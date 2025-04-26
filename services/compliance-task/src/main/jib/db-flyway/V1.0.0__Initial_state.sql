CREATE TABLE "session" (
    id varchar(50) not null,
    nonce varchar(50) not null,
    failureURL varchar(512) not null,
    successURL varchar(512) not null,
    PRIMARY KEY (id)
);
