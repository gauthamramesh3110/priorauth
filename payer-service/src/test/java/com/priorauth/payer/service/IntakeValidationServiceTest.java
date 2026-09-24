package com.priorauth.payer.service;

import com.priorauth.payer.domain.*;
import com.priorauth.payer.repository.CoverageRepository;
import com.priorauth.payer.repository.NetworkParticipationRepository;
import com.priorauth.payer.repository.PriorAuthEligibleCodeRepository;
import com.priorauth.payer.repository.ProviderRefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IntakeValidationServiceTest {

    @Mock
    private PriorAuthEligibleCodeRepository eligibleCodeRepository;

    @Mock
    private CoverageRepository coverageRepository;

    @Mock
    private ProviderRefRepository providerRepository;

    @Mock
    private NetworkParticipationRepository networkRepository;

    private IntakeValidationService service;

    private final UUID patientId = UUID.randomUUID();
    private final UUID payerId = UUID.randomUUID();
    private final UUID providerId = UUID.randomUUID();

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-09-24T12:00:00Z"),
            ZoneOffset.UTC
    );

    @BeforeEach
    public void setUp() {
        service = new IntakeValidationService(
                eligibleCodeRepository,
                coverageRepository,
                providerRepository,
                networkRepository,
                clock
        );
    }

    @Test
    void rejectsIneligibleCodeWithoutCheckingCoverageOrNetwork() {
        // Arrange: define the repository response for this scenario.
        String code = "TEST_CODE";
        CodeType codeType = CodeType.PROCEDURE;
        PriorAuthEligibleCodeId id =
                new PriorAuthEligibleCodeId(code, codeType);

        when(eligibleCodeRepository.existsById(id)).thenReturn(false);

        // Act: execute the real service.
        IntakeValidationOutcome outcome = service.validate(
                patientId,
                payerId,
                providerId,
                code,
                codeType
        );

        // Assert: verify the outcome and short-circuit behavior.
        assertEquals(IntakeResult.REJECTED, outcome.intakeResult());
        assertEquals(IntakeReason.NOT_PA_ELIGIBLE, outcome.intakeReason());

        verify(eligibleCodeRepository).existsById(id);
        verifyNoInteractions(
                coverageRepository,
                providerRepository,
                networkRepository
        );
    }

    @Test
    void rejectsEligibleCodeWithNoCoverage() {
        String code = "TEST_CODE";
        CodeType codeType = CodeType.PROCEDURE;
        PriorAuthEligibleCodeId id =
                new PriorAuthEligibleCodeId(code, codeType);
        Integer year = 2026;
        when(eligibleCodeRepository.existsById(id)).thenReturn(true);
        when(coverageRepository.findCoveragesYear(patientId, payerId, year)).thenReturn(Collections.emptyList());

        IntakeValidationOutcome outcome = service.validate(
                patientId,
                payerId,
                providerId,
                code,
                codeType
        );

        assertEquals(IntakeResult.REJECTED, outcome.intakeResult());
        assertEquals(IntakeReason.NOT_COVERED, outcome.intakeReason());

        verify(eligibleCodeRepository).existsById(id);
        verify(coverageRepository).findCoveragesYear(patientId, payerId, year);
        verifyNoInteractions(providerRepository);
        verifyNoInteractions(networkRepository);
    }

    @Test
    void rejectsOutOfNetworkWithEligibleCodeAndCoverage() {
        String code = "TEST_CODE";
        CodeType codeType = CodeType.PROCEDURE;
        PriorAuthEligibleCodeId id =
                new PriorAuthEligibleCodeId(code, codeType);
        Integer year = 2026;
        when(eligibleCodeRepository.existsById(id)).thenReturn(true);
        when(coverageRepository.findCoveragesYear(patientId, payerId, year)).thenReturn(Collections.singletonList(new Coverage(
                1L,
                patientId,
                payerId,
                2024,
                2027
        )));

        UUID organizationId = UUID.randomUUID();
        when(providerRepository.findById(providerId)).thenReturn(Optional.of(new ProviderRef(
                providerId,
                organizationId,
                "John Doe",
                "Cardio"
        )));

        LocalDate effectiveFrom = LocalDate.now(clock).minusYears(2);
        LocalDate effectiveTo = LocalDate.now(clock).plusYears(2);
        NetworkParticipationId networkParticipationId = new NetworkParticipationId(payerId, organizationId);
        when(networkRepository.findById(networkParticipationId)).thenReturn(Optional.of(new NetworkParticipation(
                networkParticipationId,
                false,
                effectiveFrom,
                effectiveTo
        )));

        IntakeValidationOutcome outcome = service.validate(
                patientId,
                payerId,
                providerId,
                code,
                codeType
        );

        assertEquals(IntakeResult.REJECTED, outcome.intakeResult());
        assertEquals(IntakeReason.OUT_OF_NETWORK, outcome.intakeReason());

        verify(eligibleCodeRepository).existsById(id);
        verify(coverageRepository).findCoveragesYear(patientId, payerId, year);
        verify(providerRepository).findById(providerId);
        verify(networkRepository).findById(networkParticipationId);
    }

    @Test
    void rejectsProviderOrParticipantMissing() {
        String code = "TEST_CODE";
        CodeType codeType = CodeType.PROCEDURE;
        PriorAuthEligibleCodeId id =
                new PriorAuthEligibleCodeId(code, codeType);
        Integer year = 2026;
        when(eligibleCodeRepository.existsById(id)).thenReturn(true);
        when(coverageRepository.findCoveragesYear(patientId, payerId, year)).thenReturn(Collections.singletonList(new Coverage(
                1L,
                patientId,
                payerId,
                2024,
                2027
        )));

        UUID organizationId = UUID.randomUUID();
        when(providerRepository.findById(providerId)).thenReturn(Optional.empty());
        IntakeValidationOutcome outcome = service.validate(
                patientId,
                payerId,
                providerId,
                code,
                codeType
        );

        assertEquals(IntakeResult.REJECTED, outcome.intakeResult());
        assertEquals(IntakeReason.OUT_OF_NETWORK, outcome.intakeReason());

        verify(eligibleCodeRepository).existsById(id);
        verify(coverageRepository).findCoveragesYear(patientId, payerId, year);
        verify(providerRepository).findById(providerId);

        verifyNoInteractions(networkRepository);
    }

    @Test
    void rejectsExpiredMembership() {
        String code = "TEST_CODE";
        CodeType codeType = CodeType.PROCEDURE;
        PriorAuthEligibleCodeId id =
                new PriorAuthEligibleCodeId(code, codeType);
        Integer year = 2026;
        when(eligibleCodeRepository.existsById(id)).thenReturn(true);
        when(coverageRepository.findCoveragesYear(patientId, payerId, year)).thenReturn(Collections.singletonList(new Coverage(
                1L,
                patientId,
                payerId,
                2024,
                2027
        )));

        UUID organizationId = UUID.randomUUID();
        when(providerRepository.findById(providerId)).thenReturn(Optional.of(new ProviderRef(
                providerId,
                organizationId,
                "John Doe",
                "Cardio"
        )));

        LocalDate effectiveFrom = LocalDate.now(clock).minusYears(2);
        LocalDate effectiveTo = LocalDate.now(clock).minusDays(1);
        NetworkParticipationId networkParticipationId = new NetworkParticipationId(payerId, organizationId);
        when(networkRepository.findById(networkParticipationId)).thenReturn(Optional.of(new NetworkParticipation(
                networkParticipationId,
                true,
                effectiveFrom,
                effectiveTo
        )));

        IntakeValidationOutcome outcome = service.validate(
                patientId,
                payerId,
                providerId,
                code,
                codeType
        );

        assertEquals(IntakeResult.REJECTED, outcome.intakeResult());
        assertEquals(IntakeReason.OUT_OF_NETWORK, outcome.intakeReason());

        verify(eligibleCodeRepository).existsById(id);
        verify(coverageRepository).findCoveragesYear(patientId, payerId, year);
        verify(providerRepository).findById(providerId);
        verify(networkRepository).findById(networkParticipationId);
    }

    @Test
    void rejectsUnstartedMembership() {
        String code = "TEST_CODE";
        CodeType codeType = CodeType.PROCEDURE;
        PriorAuthEligibleCodeId id =
                new PriorAuthEligibleCodeId(code, codeType);
        Integer year = 2026;
        when(eligibleCodeRepository.existsById(id)).thenReturn(true);
        when(coverageRepository.findCoveragesYear(patientId, payerId, year)).thenReturn(Collections.singletonList(new Coverage(
                1L,
                patientId,
                payerId,
                2024,
                2027
        )));

        UUID organizationId = UUID.randomUUID();
        when(providerRepository.findById(providerId)).thenReturn(Optional.of(new ProviderRef(
                providerId,
                organizationId,
                "John Doe",
                "Cardio"
        )));

        LocalDate effectiveFrom = LocalDate.now(clock).plusDays(1);
        LocalDate effectiveTo = LocalDate.now(clock).plusYears(6);
        NetworkParticipationId networkParticipationId = new NetworkParticipationId(payerId, organizationId);
        when(networkRepository.findById(networkParticipationId)).thenReturn(Optional.of(new NetworkParticipation(
                networkParticipationId,
                true,
                effectiveFrom,
                effectiveTo
        )));

        IntakeValidationOutcome outcome = service.validate(
                patientId,
                payerId,
                providerId,
                code,
                codeType
        );

        assertEquals(IntakeResult.REJECTED, outcome.intakeResult());
        assertEquals(IntakeReason.OUT_OF_NETWORK, outcome.intakeReason());

        verify(eligibleCodeRepository).existsById(id);
        verify(coverageRepository).findCoveragesYear(patientId, payerId, year);
        verify(providerRepository).findById(providerId);
        verify(networkRepository).findById(networkParticipationId);
    }

    @Test
    void acceptsValidMembership() {
        String code = "TEST_CODE";
        CodeType codeType = CodeType.PROCEDURE;
        PriorAuthEligibleCodeId id =
                new PriorAuthEligibleCodeId(code, codeType);
        Integer year = 2026;
        when(eligibleCodeRepository.existsById(id)).thenReturn(true);
        when(coverageRepository.findCoveragesYear(patientId, payerId, year)).thenReturn(Collections.singletonList(new Coverage(
                1L,
                patientId,
                payerId,
                2024,
                2027
        )));

        UUID organizationId = UUID.randomUUID();
        when(providerRepository.findById(providerId)).thenReturn(Optional.of(new ProviderRef(
                providerId,
                organizationId,
                "John Doe",
                "Cardio"
        )));

        LocalDate effectiveFrom = LocalDate.now(clock).minusDays(1);
        LocalDate effectiveTo = LocalDate.now(clock).plusDays(1);
        NetworkParticipationId networkParticipationId = new NetworkParticipationId(payerId, organizationId);
        when(networkRepository.findById(networkParticipationId)).thenReturn(Optional.of(new NetworkParticipation(
                networkParticipationId,
                true,
                effectiveFrom,
                effectiveTo
        )));

        IntakeValidationOutcome outcome = service.validate(
                patientId,
                payerId,
                providerId,
                code,
                codeType
        );

        assertEquals(IntakeResult.VALIDATED, outcome.intakeResult());
        assertNull(outcome.intakeReason());

        verify(eligibleCodeRepository).existsById(id);
        verify(coverageRepository).findCoveragesYear(patientId, payerId, year);
        verify(providerRepository).findById(providerId);
        verify(networkRepository).findById(networkParticipationId);
    }

    @Test
    void acceptsValidMembershipThatStartsAndEndsSameDay() {
        String code = "TEST_CODE";
        CodeType codeType = CodeType.PROCEDURE;
        PriorAuthEligibleCodeId id =
                new PriorAuthEligibleCodeId(code, codeType);
        Integer year = 2026;
        when(eligibleCodeRepository.existsById(id)).thenReturn(true);
        when(coverageRepository.findCoveragesYear(patientId, payerId, year)).thenReturn(Collections.singletonList(new Coverage(
                1L,
                patientId,
                payerId,
                2024,
                2027
        )));

        UUID organizationId = UUID.randomUUID();
        when(providerRepository.findById(providerId)).thenReturn(Optional.of(new ProviderRef(
                providerId,
                organizationId,
                "John Doe",
                "Cardio"
        )));

        LocalDate effectiveFrom = LocalDate.now(clock);
        LocalDate effectiveTo = LocalDate.now(clock);
        NetworkParticipationId networkParticipationId = new NetworkParticipationId(payerId, organizationId);
        when(networkRepository.findById(networkParticipationId)).thenReturn(Optional.of(new NetworkParticipation(
                networkParticipationId,
                true,
                effectiveFrom,
                effectiveTo
        )));

        IntakeValidationOutcome outcome = service.validate(
                patientId,
                payerId,
                providerId,
                code,
                codeType
        );

        assertEquals(IntakeResult.VALIDATED, outcome.intakeResult());
        assertNull(outcome.intakeReason());

        verify(eligibleCodeRepository).existsById(id);
        verify(coverageRepository).findCoveragesYear(patientId, payerId, year);
        verify(providerRepository).findById(providerId);
        verify(networkRepository).findById(networkParticipationId);
    }



}
