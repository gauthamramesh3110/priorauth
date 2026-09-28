-- Provider-owned reference data, seeded from the same working set as the payer.
-- This service has no connection or foreign keys to payer_db.
CREATE TABLE patient_ref (
    id         UUID        PRIMARY KEY,
    first_name VARCHAR(64) NOT NULL,
    last_name  VARCHAR(64) NOT NULL,
    birthdate  DATE        NOT NULL
);

CREATE TABLE organization_ref (
    id   UUID         PRIMARY KEY,
    name VARCHAR(128) NOT NULL
);

CREATE TABLE provider_ref (
    id              UUID         PRIMARY KEY,
    organization_id UUID         NOT NULL,
    name            VARCHAR(128) NOT NULL,
    specialty       VARCHAR(64),

    CONSTRAINT fk_provider_organization
        FOREIGN KEY (organization_id) REFERENCES organization_ref (id)
);

-- Local copy of the curated eligible-code list used for pre-submit validation.
CREATE TABLE prior_auth_eligible_code (
    code        VARCHAR(32)  NOT NULL,
    code_type   VARCHAR(16)  NOT NULL,
    description VARCHAR(256) NOT NULL,

    CONSTRAINT pk_eligible_code PRIMARY KEY (code, code_type)
);
