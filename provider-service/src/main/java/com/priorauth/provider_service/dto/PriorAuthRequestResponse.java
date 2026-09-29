package com.priorauth.provider_service.dto;

import java.util.UUID;

public record PriorAuthRequestResponse(
        PriorAuthRequestStatus status,
        UUID requestId,
        PriorAuthRequestStatus errorCode
) {
    public PriorAuthRequestResponse(PriorAuthRequestStatus status) {
        this(status, null, status == PriorAuthRequestStatus.SUBMITTED ? null : status);
    }
}