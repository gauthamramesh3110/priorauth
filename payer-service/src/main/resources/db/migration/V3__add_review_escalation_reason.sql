-- Keep the payer's escalation reason separate from the provider's clinical reason_code.
-- Nullable because reviews that have not been escalated have no escalation reason.
ALTER TABLE prior_auth_review
    ADD COLUMN escalation_reason VARCHAR(64);
