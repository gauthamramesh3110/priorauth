package com.priorauth.payer.service;

import com.priorauth.payer.domain.*;
import com.priorauth.payer.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicalReviewServiceTest {
    @Mock private PatientConditionRepository conditions;
    @Mock private PriorAuthCriteriaRepository criteria;
    @Mock private PriorAuthReviewRepository reviews;
    private ClinicalReviewService service;
    private PriorAuthReview review;

    @BeforeEach
    void setUp() {
        // Exercise the real evaluator and decision service; only persistence is mocked.
        DecisionService decisions = new DecisionService(
                Clock.fixed(Instant.parse("2026-09-27T12:00:00Z"), ZoneOffset.UTC), reviews);
        service = new ClinicalReviewService(conditions, criteria, reviews, decisions, new ClinicalEvaluator());
        review = new PriorAuthReview(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "TEST_PROCEDURE", CodeType.PROCEDURE,
                Instant.parse("2026-09-26T00:30:00Z"));
    }

    @Test
    void qualifyingConditionProducesSavedAutoApproval() {
        when(criteria.findByCodeAndCodeType("TEST_PROCEDURE", CodeType.PROCEDURE))
                .thenReturn(Optional.of(criterion(true)));
        when(conditions.findByPatientIdAndCode(review.getPatientId(), "44054006"))
                .thenReturn(List.of(condition()));

        service.review(review);

        assertAll(
                () -> assertEquals(ReviewTier.AUTO, review.getReviewTier()),
                () -> assertEquals(Decision.APPROVED, review.getDecision()),
                () -> assertEquals("AUTO_APPROVED", review.getDecisionReason()),
                () -> assertEquals(Instant.parse("2026-09-27T12:00:00Z"), review.getDecidedAt()),
                () -> assertEquals(Instant.parse("2026-10-27T12:00:00Z"), review.getExpiresAt()),
                () -> assertNull(review.getExpiredAt()));
        verify(conditions).findByPatientIdAndCode(review.getPatientId(), "44054006");
        verify(reviews).save(review);
    }

    @ParameterizedTest
    @EnumSource(EscalationReason.class)
    void escalationSavesPhysicianTierAndExactReasonWithoutDecision(EscalationReason reason) {
        Optional<PriorAuthCriteria> criterion = reason == EscalationReason.NO_CRITERION_DEFINED
                ? Optional.empty()
                : Optional.of(criterion(reason != EscalationReason.ALWAYS_PHYSICIAN_REVIEW));
        when(criteria.findByCodeAndCodeType("TEST_PROCEDURE", CodeType.PROCEDURE)).thenReturn(criterion);
        // Mockito's default empty list models no matching patient conditions.

        service.review(review);

        assertAll(
                () -> assertEquals(ReviewTier.PHYSICIAN, review.getReviewTier()),
                () -> assertEquals(reason.name(), review.getReasonCode()),
                () -> assertNull(review.getDecision()),
                () -> assertNull(review.getDecisionReason()),
                () -> assertNull(review.getDecidedAt()),
                () -> assertNull(review.getExpiresAt()),
                () -> assertNull(review.getExpiredAt()));
        verify(reviews).save(review);
    }

    private static PriorAuthCriteria criterion(boolean autoApprove) {
        PriorAuthCriteria criterion = new PriorAuthCriteria(1L, "TEST_PROCEDURE", CodeType.PROCEDURE,
                "Requires condition", autoApprove, 30);
        ReflectionTestUtils.setField(criterion, "requiredConditionCode", "44054006");
        return criterion;
    }

    private static PatientCondition condition() {
        PatientCondition condition = new PatientCondition();
        ReflectionTestUtils.setField(condition, "code", "44054006");
        ReflectionTestUtils.setField(condition, "onsetDate", LocalDate.of(2026, 9, 25));
        ReflectionTestUtils.setField(condition, "resolvedDate", LocalDate.of(2026, 9, 27));
        return condition;
    }
}