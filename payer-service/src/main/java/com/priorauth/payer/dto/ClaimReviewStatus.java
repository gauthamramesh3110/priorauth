package com.priorauth.payer.dto;

public enum ClaimReviewStatus {
    ALREADY_OWNED,
    OWNED_BY_ANOTHER_REVIEWER,
    NOT_PHYSICIAN_TIER,
    CLAIMED,
    ALREADY_DECIDED
}
