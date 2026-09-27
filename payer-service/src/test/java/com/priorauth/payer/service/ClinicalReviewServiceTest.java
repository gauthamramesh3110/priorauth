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
        Clock clock = Clock.fixed(Instant.parse("2026-09-27T12:00:00Z"), ZoneOffset.UTC);
        // Exercise the real evaluator and decision service; only persistence is mocked.
        DecisionService decisions = new DecisionService(
               clock, reviews);
        service = new ClinicalReviewService(conditions, criteria, reviews, decisions, new ClinicalEvaluator(), clock);
        review = new PriorAuthReview(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "TEST_PROCEDURE", CodeType.PROCEDURE,
                Instant.parse("2026-09-26T00:30:00Z"), "Further testing requested after an abnormal observation.");
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
                () -> assertEquals("Further testing requested after an abnormal observation.", review.getReason()),
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
                () -> assertEquals(reason, review.getEscalationReason()),
                () -> assertEquals("Further testing requested after an abnormal observation.", review.getReason()),
                () -> assertNull(review.getDecision()),
                () -> assertNull(review.getDecisionReason()),
                () -> assertNull(review.getDecidedAt()),
                () -> assertNull(review.getExpiresAt()),
                () -> assertNull(review.getExpiredAt()));
        verify(reviews).save(review);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.NullSource
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "Further testing requested after an abnormal observation.",
            "Symptoms persist.\nPlease review the attached clinical evidence."
    })
    void queueReturnsProviderReasonWithoutConditionLookup(String reason) {
        PriorAuthReview queueReview = new PriorAuthReview(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "US", CodeType.IMAGING,
                Instant.parse("2026-09-26T00:30:00Z"), reason);
        queueReview.escalateToPhysician(EscalationReason.NO_CRITERION_DEFINED);
        PatientRef patient = mock(PatientRef.class);
        ProviderRef provider = mock(ProviderRef.class);
        PriorAuthEligibleCode code = mock(PriorAuthEligibleCode.class);
        when(patient.getFirstName()).thenReturn("Test");
        when(patient.getLastName()).thenReturn("Patient");
        when(provider.getName()).thenReturn("Test Provider");
        when(provider.getSpecialty()).thenReturn("GENERAL PRACTICE");
        when(code.getDescription()).thenReturn("Ultrasound");
        ReflectionTestUtils.setField(queueReview, "patientRef", patient);
        ReflectionTestUtils.setField(queueReview, "providerRef", provider);
        ReflectionTestUtils.setField(queueReview, "priorAuthEligibleCode", code);
        var pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        when(reviews.findByReviewerIdAndReviewTierAndExpiresAtBefore(null, ReviewTier.PHYSICIAN,
                Instant.parse("2026-10-04T12:00:00Z"), pageable)).thenReturn(List.of(queueReview));

        var queue = service.getReviewQueue(ReviewTier.PHYSICIAN, null, 7, pageable);

        assertEquals(1, queue.content().size());
        assertEquals(reason, queue.content().getFirst().reason());
        assertEquals(EscalationReason.NO_CRITERION_DEFINED, queue.content().getFirst().escalationReason());
        verifyNoInteractions(conditions, criteria);
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