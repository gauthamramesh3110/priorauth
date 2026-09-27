package com.priorauth.payer.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

import com.priorauth.payer.event.ReviewDecidedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
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
    private final PriorAuthReviewRepository priorAuthReviewRepository;
    private final ApplicationEventPublisher publisher;

    @Value("${payer.approval.default-validity-days}")
    int defaultValidityDays;

    public DecisionService(Clock clock, PriorAuthReviewRepository priorAuthReviewRepository, ApplicationEventPublisher publisher) {
        this.clock = clock;
        this.priorAuthReviewRepository = priorAuthReviewRepository;
        this.publisher = publisher;
    }

    @Transactional
    public void decide(PriorAuthReview review, Decision decision, String decisionReason, Integer defaultValidityDays) {
        Instant decidedAt = Instant.now(clock);

        if (decision.equals(Decision.APPROVED)) {
            Instant expiresAt = decidedAt.plus(
                    defaultValidityDays != null ? defaultValidityDays : this.defaultValidityDays, ChronoUnit.DAYS);
            review.setDecision(decision, decisionReason, decidedAt, expiresAt);
        } else {
            review.setDecision(decision, decisionReason, decidedAt, null);
        }

        priorAuthReviewRepository.save(review);
        publisher.publishEvent(new ReviewDecidedEvent(
                review.getRequestId(),
                review.getDecision(),
                review.getDecisionReason(),
                review.getDecidedAt(),
                review.getExpiresAt()
        ));
    }
}
