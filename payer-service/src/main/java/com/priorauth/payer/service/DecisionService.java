package com.priorauth.payer.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.priorauth.payer.domain.Decision;
import com.priorauth.payer.domain.PriorAuthCriteria;
import com.priorauth.payer.domain.PriorAuthReview;
import com.priorauth.payer.domain.ReviewTier;
import com.priorauth.payer.repository.PriorAuthReviewRepository;

@Service
public class DecisionService {

    private final Clock clock;
    private PriorAuthReviewRepository priorAuthReviewRepository;

    public DecisionService(Clock clock, PriorAuthReviewRepository priorAuthReviewRepository) {
        this.clock = clock;
        this.priorAuthReviewRepository = priorAuthReviewRepository;
    }

    @Transactional
    public void decide(PriorAuthReview review, Decision decision, String decisionReason, PriorAuthCriteria criteria) {
        ReviewTier reviewTier = review.getReviewTier();
        String reasonCode = review.getReasonCode();
        Instant decidedAt = Instant.now(clock);

        if (decision.equals(Decision.APPROVED)) {
            Instant expiresAt = decidedAt.plus(
                    criteria.getDefaultValidityDays(), ChronoUnit.DAYS);
            review.setDecision(decision, decisionReason, decidedAt, expiresAt);
        } else {
            review.setDecision(decision, decisionReason, decidedAt, null);
        }

        priorAuthReviewRepository.save(review);

    }
}
