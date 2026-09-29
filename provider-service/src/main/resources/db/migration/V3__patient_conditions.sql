-- Local clinical history from the committed Synthea working set.
CREATE TABLE patient_condition (
    id            BIGSERIAL PRIMARY KEY,
    patient_id    UUID NOT NULL REFERENCES patient_ref (id),
    encounter_id  UUID,
    code          VARCHAR(32) NOT NULL,
    description   VARCHAR(256),
    onset_date    DATE NOT NULL,
    resolved_date DATE,
    CONSTRAINT ck_condition_dates CHECK (resolved_date IS NULL OR resolved_date >= onset_date)
);

CREATE INDEX idx_patient_condition_history
    ON patient_condition (patient_id, onset_date DESC, id);
