package com.priorauth.provider_service.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class PriorAuthRequest {
    public PriorAuthRequest(
            UUID id,
            UUID patientId,
            UUID providerId,
            UUID organizationId,
            UUID payerId,
            String requestedCode,
            CodeType codeType,
            Instant submittedAt,
            String reason,
            RequestStatus status,
            String statusReason,
            String submittedBy
        ) {
        this.reason = reason;
        this.id = Objects.requireNonNull(id, "requestId is required");
        this.patientId = Objects.requireNonNull(patientId, "patientId is required");
        this.providerId = Objects.requireNonNull(providerId, "providerId is required");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.payerId = Objects.requireNonNull(payerId, "payerId is required");
        this.requestedCode = Objects.requireNonNull(requestedCode, "requestedCode is required");
        this.codeType = Objects.requireNonNull(codeType, "codeType is required");
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.statusReason = statusReason;
        this.submittedBy = Objects.requireNonNull(submittedBy, "submittedBy is required");
    }

    /** Apply the synchronous PA-29 bridge outcome before committing the request. */
    public void recordPayerResult(RequestStatus status, String reason, Instant decidedAt, Instant expiresAt) {
        if (status != RequestStatus.INTAKE_REJECTED && status != RequestStatus.IN_REVIEW
                && status != RequestStatus.APPROVED && status != RequestStatus.DENIED) {
            throw new IllegalArgumentException("Unexpected payer result: " + status);
        }
        this.status = status;
        this.statusReason = reason;
        this.decidedAt = decidedAt;
        this.expiresAt = expiresAt;
    }

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private UUID payerId;

    @Column(length = 32, nullable = false)
    private String requestedCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private CodeType codeType;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private RequestStatus status;

    @Column(length = 256)
    private String statusReason;

    @Column(nullable = false)
    private Instant submittedAt;
    private Instant decidedAt;
    private Instant expiresAt;
    private UUID appealOf;
    private String submittedBy;
}
