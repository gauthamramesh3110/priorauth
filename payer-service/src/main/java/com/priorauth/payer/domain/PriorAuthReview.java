package com.priorauth.payer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
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
            Instant submittedAt) {
        this(requestId, patientId, providerId, organizationId, payerId, requestedCode, codeType, submittedAt, null);
    }

    public PriorAuthReview(
            UUID requestId,
            UUID patientId,
            UUID providerId,
            UUID organizationId,
            UUID payerId,
            String requestedCode,
            CodeType codeType,
            Instant submittedAt,
            String reason) {
        this.reason = reason;
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
    @Column(name = "patient_id", nullable = false, insertable = false, updatable = false)
    private UUID patientId;
    @Column(name = "provider_id", nullable = false, insertable = false, updatable = false)
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
    @Column(name = "escalation_reason", length = 64)
    private EscalationReason escalationReason;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private PatientRef patientRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private ProviderRef providerRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "requestedCode", referencedColumnName = "code", insertable = false, updatable = false),
            @JoinColumn(name = "codeType", referencedColumnName = "codeType", insertable = false, updatable = false)
    })
    private PriorAuthEligibleCode priorAuthEligibleCode;

    public void escalateToPhysician(EscalationReason reason) {
        Objects.requireNonNull(reason, "physician escalation requires a reason");

        if (decision != null) {
            throw new IllegalStateException("Review decision has already been made");
        }

        this.reviewTier = ReviewTier.PHYSICIAN;
        this.escalationReason = reason;
    }

    public void markForAutoApproval() {
        if (decision != null) {
            throw new IllegalStateException("Review decision has already been made");
        }

        this.reviewTier = ReviewTier.AUTO;
    }

    public void setDecision(Decision decision, String decisionReason, Instant decidedAt, Instant expiresAt) {
        if (this.decision != null) {
            throw new IllegalStateException("Review decision has already been made");
        }

        if (decision == null) {
            throw new IllegalArgumentException("decision is required to set decision");
        }

        if (decisionReason == null) {
            throw new IllegalArgumentException("decision reason is required to set decision");
        }

        if (decidedAt == null) {
            throw new IllegalArgumentException("decision date is required to set decision");
        }

        this.decision = decision;
        this.decisionReason = decisionReason;
        this.decidedAt = decidedAt;
        this.expiresAt = expiresAt;
    }
}
