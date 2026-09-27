-- The provider justification is an optional narrative snapshot, not a condition lookup.
-- Rename and widen the existing column to retain existing values and nulls verbatim.
ALTER TABLE prior_auth_review RENAME COLUMN reason_code TO reason;
ALTER TABLE prior_auth_review ALTER COLUMN reason TYPE TEXT;
