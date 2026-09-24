package com.priorauth.payer.service;

import com.priorauth.payer.domain.*;
import com.priorauth.payer.repository.CoverageRepository;
import com.priorauth.payer.repository.NetworkParticipationRepository;
import com.priorauth.payer.repository.PriorAuthEligibleCodeRepository;
import com.priorauth.payer.repository.ProviderRefRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IntakeValidationService {
    PriorAuthEligibleCodeRepository priorAuthEligibleCodeRepository;
    CoverageRepository coverageRepository;
    ProviderRefRepository providerRefRepository;
    NetworkParticipationRepository networkParticipationRepository;
    Clock clock;

    public IntakeValidationService(
            PriorAuthEligibleCodeRepository priorAuthEligibleCodeRepository,
            CoverageRepository coverageRepository,
            ProviderRefRepository providerRefRepository,
            NetworkParticipationRepository networkParticipationRepository,
            Clock clock
    ) {
        this.priorAuthEligibleCodeRepository = priorAuthEligibleCodeRepository;
        this.coverageRepository = coverageRepository;
        this.providerRefRepository = providerRefRepository;
        this.networkParticipationRepository = networkParticipationRepository;
        this.clock = clock;
    }

    public IntakeValidationOutcome validate(
            UUID patientId,
            UUID payerId,
            UUID providerId,
            String requestCode,
            CodeType requestCodeType
    ) {
        LocalDate today = LocalDate.now(clock);
        boolean codePresent = this.priorAuthEligibleCodeRepository.existsById(new PriorAuthEligibleCodeId(requestCode, requestCodeType));
        if (!codePresent) {
            return new IntakeValidationOutcome(IntakeResult.REJECTED, IntakeReason.NOT_PA_ELIGIBLE);
        }


        Integer currentYear = today.getYear();
        List<Coverage> coverages = coverageRepository.findCoveragesYear(patientId, payerId, currentYear);
        if (coverages.isEmpty()) {
            return new IntakeValidationOutcome(IntakeResult.REJECTED, IntakeReason.NOT_COVERED);
        }

        Optional<ProviderRef> providerRef = providerRefRepository.findById(providerId);
        UUID organizationId = providerRef.isPresent() ? providerRef.get().getOrganizationId() : null;
        if (organizationId == null) {
            return new IntakeValidationOutcome(IntakeResult.REJECTED, IntakeReason.OUT_OF_NETWORK);
        }

        Optional<NetworkParticipation> networkParticipation = networkParticipationRepository.findById(new NetworkParticipationId(payerId, organizationId));
        if (networkParticipation.isEmpty()) {
            return new IntakeValidationOutcome(IntakeResult.REJECTED, IntakeReason.OUT_OF_NETWORK);

        }

        NetworkParticipation participation = networkParticipation.get();
        boolean isInNetwork = participation.getInNetwork();
        boolean hasExpired = participation.getEffectiveTo() != null && participation.getEffectiveTo().isBefore(today);
        boolean startsInFuture = participation.getEffectiveFrom().isAfter(today);

        if (!isInNetwork || startsInFuture || hasExpired) {
            return new IntakeValidationOutcome(IntakeResult.REJECTED, IntakeReason.OUT_OF_NETWORK);
        }

        return new IntakeValidationOutcome(IntakeResult.VALIDATED, null);
    }
}
