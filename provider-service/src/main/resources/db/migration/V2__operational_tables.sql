-- Provider Service operational schema. Reference tables are created in V1.

-- Consumers insert into this ledger in the same transaction as their state change.
CREATE TABLE processed_event (
    event_id    UUID        PRIMARY KEY,
    topic       VARCHAR(64) NOT NULL,
    consumed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- IDs are minted by the provider at submission and shared with the payer.
-- payer_id is an external identifier, not a cross-database foreign key.
-- reason is optional provider narrative, separate from the resulting status_reason.
CREATE TABLE prior_auth_request (
    id              UUID         PRIMARY KEY,
    patient_id      UUID         NOT NULL,
    provider_id     UUID         NOT NULL,
    organization_id UUID         NOT NULL,
    payer_id        UUID         NOT NULL,
    requested_code  VARCHAR(32)  NOT NULL,
    code_type       VARCHAR(16)  NOT NULL,
    reason          TEXT,
    status          VARCHAR(32)  NOT NULL,
    status_reason   VARCHAR(256),
    submitted_at    TIMESTAMPTZ  NOT NULL,
    decided_at      TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,
    appeal_of       UUID,
    submitted_by    VARCHAR(256) NOT NULL,

    CONSTRAINT fk_request_patient
        FOREIGN KEY (patient_id) REFERENCES patient_ref (id),
    CONSTRAINT fk_request_provider
        FOREIGN KEY (provider_id) REFERENCES provider_ref (id),
    CONSTRAINT fk_request_organization
        FOREIGN KEY (organization_id) REFERENCES organization_ref (id),
    CONSTRAINT fk_request_eligible_code
        FOREIGN KEY (requested_code, code_type)
            REFERENCES prior_auth_eligible_code (code, code_type),
    CONSTRAINT fk_request_appeal_of
        FOREIGN KEY (appeal_of) REFERENCES prior_auth_request (id)
);

-- Appeals create a new row linked to the original denied request.
CREATE INDEX idx_request_patient_status ON prior_auth_request (patient_id, status);
CREATE INDEX idx_request_status ON prior_auth_request (status);
CREATE INDEX idx_request_appeal_of ON prior_auth_request (appeal_of);

CREATE TABLE notification (
    id         BIGSERIAL    PRIMARY KEY,
    request_id UUID         NOT NULL,
    type       VARCHAR(32)  NOT NULL,
    message    VARCHAR(512) NOT NULL,
    recipient  VARCHAR(256) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    read_at    TIMESTAMPTZ,

    CONSTRAINT fk_notification_request
        FOREIGN KEY (request_id) REFERENCES prior_auth_request (id)
);
