-- V1__operational_tables.sql
-- Payer Service operational schema.
-- These are the tables the application writes to. Curated configuration and
-- seeded reference data arrive in V2.

-- ---------------------------------------------------------------------------
-- processed_event
--
-- Consumer-side idempotency ledger. Kafka delivers at least once, so every
-- consumer checks this table before acting and inserts into it within the same
-- transaction as its state change.
-- ---------------------------------------------------------------------------
CREATE TABLE processed_event (
     event_id    UUID        PRIMARY KEY,
     topic       VARCHAR(64) NOT NULL,
     consumed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- prior_auth_review
--
-- One row per request the payer has seen. The primary key is the request ID
-- minted by the Provider Service, so both services agree on identity without a
-- shared sequence.
--
-- patient_id, provider_id, organization_id and payer_id are deliberately plain
-- UUIDs with no foreign key. The tables they point at are denormalised caches
-- of another organization's data, and a hard constraint against a cache is one
-- you end up fighting.
--
-- Most columns are nullable on purpose. NULL is the meaningful state: a null
-- decision means not yet decided, and a null expired_at is the guard that makes
-- the expiration sweep idempotent.
-- ---------------------------------------------------------------------------
CREATE TABLE prior_auth_review (
       request_id      UUID          PRIMARY KEY,

       patient_id      UUID          NOT NULL,
       provider_id     UUID          NOT NULL,
       organization_id UUID          NOT NULL,
       payer_id        UUID          NOT NULL,

       requested_code  VARCHAR(32)   NOT NULL,
       code_type       VARCHAR(16)   NOT NULL,
       reason_code     VARCHAR(32),

       intake_result   VARCHAR(16),
       intake_reason   VARCHAR(64),

       review_tier     VARCHAR(16),
       reviewer_id     UUID,

       decision        VARCHAR(16),
       decision_reason VARCHAR(256),

       submitted_at    TIMESTAMPTZ   NOT NULL,
       decided_at      TIMESTAMPTZ,
       expires_at      TIMESTAMPTZ,
       expired_at      TIMESTAMPTZ,

       appeal_of       UUID,

       CONSTRAINT fk_review_appeal_of
           FOREIGN KEY (appeal_of) REFERENCES prior_auth_review (request_id)
);

-- Queue read: requests sitting at physician tier with no decision yet.
CREATE INDEX idx_review_queue ON prior_auth_review (review_tier, decision);

-- Expiration sweep: approved requests past their expiry.
CREATE INDEX idx_review_expiry ON prior_auth_review (decision, expires_at);

-- Patient lookups from the review context endpoint.
CREATE INDEX idx_review_patient ON prior_auth_review (patient_id);