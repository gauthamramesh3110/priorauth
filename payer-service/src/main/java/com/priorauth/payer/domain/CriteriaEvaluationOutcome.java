package com.priorauth.payer.domain;

import java.util.Objects;

public record CriteriaEvaluationOutcome(EvaluationResult result, EscalationReason reason) {
    public CriteriaEvaluationOutcome {
        Objects.requireNonNull(result, "result is required");

        if (result == EvaluationResult.AUTO_APPROVE && reason != null) {
            throw new IllegalArgumentException(
                    "Auto Approved evaluation result must not have a rejection reason");
        }

        if (result == EvaluationResult.ESCALATE && reason == null) {
            throw new IllegalArgumentException(
                    "Escalated evaluation requires a reason");
        }
    }
}
