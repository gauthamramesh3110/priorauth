package com.priorauth.provider_service.dto;

import com.priorauth.provider_service.domain.CodeType;
import com.priorauth.provider_service.domain.RequestStatus;

import java.time.Instant;
import java.util.UUID;

public record PriorAuthRequestItem(
        UUID requestId,
        UUID patientId,
        UUID providerId,
        UUID organizationId,
        String requestedCode,
        CodeType codeType,
        String reason,
        RequestStatus requestStatus,
        String statusReason,
        Instant submittedAt,
        Instant decidedAt,
        Instant expiresAt,
        UUID appealOf,
        String submittedBy
) {
}
