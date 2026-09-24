package com.priorauth.payer.domain;

import java.util.Objects;

public record IntakeValidationOutcome(
        IntakeResult intakeResult,
        IntakeReason intakeReason
) {
    public IntakeValidationOutcome {
        Objects.requireNonNull(intakeResult, "result is required");

        if (intakeResult == IntakeResult.VALIDATED && intakeReason != null) {
            throw new IllegalArgumentException(
                    "Validated intake must not have a rejection reason");
        }

        if (intakeResult == IntakeResult.REJECTED && intakeReason == null) {
            throw new IllegalArgumentException(
                    "Rejected intake requires a reason");
        }
    }
}
