package com.priorauth.payer.service;

import com.priorauth.payer.domain.*;
import com.priorauth.payer.repository.PriorAuthReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-26T12:34:56Z");
    @Mock private PriorAuthReviewRepository repository;
    private DecisionService service;

    @BeforeEach
    void setUp() {
        service = new DecisionService(Clock.fixed(NOW, ZoneOffset.UTC), repository);
    }

    @ParameterizedTest
    @CsvSource({"1, 2026-09-27T12:34:56Z", "30, 2026-10-26T12:34:56Z",
            "365, 2027-09-26T12:34:56Z"})
    void approvalUsesCriterionValidityFromDecisionTime(int days, String expectedExpiry) {
        PriorAuthReview review = review();
        review.markForAutoApproval();
        PriorAuthCriteria criterion = criterion(days);

        service.decide(review, Decision.APPROVED, "Required condition met", criterion);

        assertAll(
                () -> assertEquals(Decision.APPROVED, review.getDecision()),
                () -> assertEquals("Required condition met", review.getDecisionReason()),
                () -> assertEquals(NOW, review.getDecidedAt()),
                () -> assertEquals(Instant.parse(expectedExpiry), review.getExpiresAt()),
                () -> assertNull(review.getExpiredAt()),
                () -> assertEquals(ReviewTier.AUTO, review.getReviewTier()));
        verify(repository).save(review);
    }

    @Test
    void manualApprovalPreservesPhysicianTier() {
        PriorAuthReview review = review();
        review.escalateToPhysician(EscalationReason.ALWAYS_PHYSICIAN_REVIEW);

        service.decide(review, Decision.APPROVED, "Approved after review", criterion(30));

        assertEquals(ReviewTier.PHYSICIAN, review.getReviewTier());
        assertEquals(Decision.APPROVED, review.getDecision());
        assertEquals(Instant.parse("2026-10-26T12:34:56Z"), review.getExpiresAt());
        verify(repository).save(review);
    }

    @Test
    void denialRecordsDecisionWithoutExpiryOrCriterion() {
        PriorAuthReview review = review();
        review.escalateToPhysician(EscalationReason.NO_CRITERION_DEFINED);

        service.decide(review, Decision.DENIED, "Insufficient supporting evidence", null);

        assertAll(
                () -> assertEquals(Decision.DENIED, review.getDecision()),
                () -> assertEquals("Insufficient supporting evidence", review.getDecisionReason()),
                () -> assertEquals(NOW, review.getDecidedAt()),
                () -> assertNull(review.getExpiresAt()),
                () -> assertNull(review.getExpiredAt()));
        verify(repository).save(review);
    }

    @Test
    void cannotOverwriteAnExistingDecision() {
        PriorAuthReview review = review();
        Instant originalTime = Instant.parse("2026-09-25T10:00:00Z");
        review.setDecision(Decision.DENIED, "Original reason", originalTime, null);

        assertThrows(IllegalStateException.class,
                () -> service.decide(review, Decision.APPROVED, "Replacement", criterion(30)));

        assertEquals(Decision.DENIED, review.getDecision());
        assertEquals("Original reason", review.getDecisionReason());
        assertEquals(originalTime, review.getDecidedAt());
        assertNull(review.getExpiresAt());
        verifyNoInteractions(repository);
    }

    @Test
    void missingReasonDoesNotMutateOrSaveReview() {
        PriorAuthReview review = review();
        assertThrows(IllegalArgumentException.class,
                () -> service.decide(review, Decision.APPROVED, null, criterion(30)));
        assertNull(review.getDecision());
        assertNull(review.getDecidedAt());
        assertNull(review.getExpiresAt());
        verifyNoInteractions(repository);
    }

    private static PriorAuthCriteria criterion(int days) {
        return new PriorAuthCriteria(1L, "TEST_PROCEDURE", CodeType.PROCEDURE,
                "Requires condition", true, days);
    }

    private static PriorAuthReview review() {
        return new PriorAuthReview(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "TEST_PROCEDURE", CodeType.PROCEDURE,
                Instant.parse("2026-09-20T00:00:00Z"));
    }
}