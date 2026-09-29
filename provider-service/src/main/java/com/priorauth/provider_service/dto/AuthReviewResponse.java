package com.priorauth.provider_service.dto;
import com.priorauth.provider_service.domain.RequestStatus;
import java.time.Instant;
import java.util.UUID;
/** Temporary PA-29 result; remove with the REST bridge in PA-35. */
public record AuthReviewResponse(UUID requestId, RequestStatus status, String statusReason,
                                 Instant decidedAt, Instant expiresAt) {}
