CREATE TABLE RequestObject (
    exp TIMESTAMP WITH TIME ZONE,
    authReqId VARCHAR(255) NOT NULL,
    profileId VARCHAR(255),
    requestObjectJwt VARCHAR(102400),
    taskName VARCHAR(255),
    failureUri BYTEA,
    requestObject JSONB,
    successUri BYTEA,
    PRIMARY KEY (authReqId)
);
