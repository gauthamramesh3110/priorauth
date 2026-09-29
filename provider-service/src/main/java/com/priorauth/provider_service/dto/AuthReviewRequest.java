package com.priorauth.provider_service.dto;
import com.priorauth.provider_service.domain.CodeType;
import java.time.Instant;
import java.util.UUID;
/** Temporary PA-29 wire contract, owned independently from payer-service. */
public record AuthReviewRequest(
        UUID requestId, UUID patientId, UUID providerId, UUID organizationId, UUID payerId,
        String requestedCode, CodeType codeType, String reason, Instant submittedAt
) {}
