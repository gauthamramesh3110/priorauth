package com.priorauth.payer.dto;
import java.time.Instant;
import java.util.UUID;
/** Temporary PA-29 wire result; does not expose payer-internal review tiers. */
public record AuthReviewResponse(UUID requestId, String status, String statusReason,
                                 Instant decidedAt, Instant expiresAt) {}
