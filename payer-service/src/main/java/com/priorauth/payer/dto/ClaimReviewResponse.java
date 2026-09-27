package com.priorauth.payer.dto;

import java.util.UUID;

public record ClaimReviewResponse(
        UUID reviewerId,
        UUID requestId,
        ClaimReviewStatus status
) {
}
