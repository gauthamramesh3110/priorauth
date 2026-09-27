package com.priorauth.payer.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ClaimReviewRequest(
        @NotNull  UUID reviewerId
) {
}
