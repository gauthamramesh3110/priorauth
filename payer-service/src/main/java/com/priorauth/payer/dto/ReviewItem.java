package com.priorauth.payer.dto;

import com.priorauth.payer.domain.CodeType;
import com.priorauth.payer.domain.EscalationReason;
import com.priorauth.payer.domain.ReviewTier;

import java.time.Instant;
import java.util.UUID;

public record ReviewItem(
        UUID requestId,
        String patientName,
        UUID patientId,
        String providerName,
        String providerSpecialty,
        String requestCode,
        CodeType codeType,
        String codeDescription,
        String reason,
        ReviewTier reviewTier,
        UUID reviewerId,
        Instant submittedAt,
        EscalationReason escalationReason,
        Boolean isAppeal,
        UUID appealOf
) {
}
