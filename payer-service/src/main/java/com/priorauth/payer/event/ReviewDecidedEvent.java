package com.priorauth.payer.event;

import com.priorauth.payer.domain.Decision;

import java.time.Instant;
import java.util.UUID;

public record ReviewDecidedEvent(
        UUID requestId,
        Decision decision,
        String decisionReason,
        Instant decidedAt,
        Instant expiresAt
) {}