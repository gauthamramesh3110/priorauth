package com.priorauth.payer.domain;

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
public class PriorAuthReview {

    public PriorAuthReview(
            UUID requestId,
            UUID patientId,
            UUID providerId,
            UUID organizationId,
            UUID payerId,
            String requestedCode,
            CodeType codeType,
            Instant submittedAt
    ) {
        this.requestId = Objects.requireNonNull(requestId, "requestId is required");
        this.patientId = Objects.requireNonNull(patientId, "patientId is required");
        this.providerId = Objects.requireNonNull(providerId, "providerId is required");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.payerId = Objects.requireNonNull(payerId, "payerId is required");
        this.requestedCode = Objects.requireNonNull(requestedCode, "requestedCode is required");
        this.codeType = Objects.requireNonNull(codeType, "codeType is required");
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt is required");
    }

    @Id
    private UUID requestId;
    @Column(nullable = false)
    private UUID patientId;
    @Column(nullable = false)
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
    @Column(length = 32)
    private String reasonCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private IntakeResult intakeResult;
    @Enumerated(EnumType.STRING)
    @Column(length = 64)
    private IntakeReason intakeReason;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ReviewTier reviewTier;
    private UUID reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Decision decision;
    @Column(length = 256)
    private String decisionReason;

    @Column(nullable = false)
    private Instant submittedAt;
    private Instant decidedAt;
    private Instant expiresAt;
    private Instant expiredAt;

    private UUID appealOf;
}
