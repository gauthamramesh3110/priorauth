package com.priorauth.provider_service.service;

import com.priorauth.provider_service.domain.*;
import com.priorauth.provider_service.dto.*;
import com.priorauth.provider_service.httpclient.PayerClient;
import com.priorauth.provider_service.repository.PatientRefRepository;
import com.priorauth.provider_service.repository.PriorAuthEligibleCodeRepository;
import com.priorauth.provider_service.repository.PriorAuthRequestRepository;
import com.priorauth.provider_service.repository.ProviderRefRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PriorAuthRequestService {
    private static final String TEMP_USER_ID = "11111111-1111-1111-1111-111111111111";

    private final PatientRefRepository patientRefRepository;
    private final ProviderRefRepository providerRefRepository;
    private final PriorAuthEligibleCodeRepository priorAuthEligibleCodeRepository;
    private final PriorAuthRequestRepository priorAuthRequestRepository;
    private final PayerClient payerClient;
    private final Clock clock;

    public PriorAuthRequestService(PatientRefRepository patientRefRepository, ProviderRefRepository providerRefRepository, PriorAuthEligibleCodeRepository priorAuthEligibleCodeRepository, PriorAuthRequestRepository priorAuthRequestRepository, PayerClient payerClient, Clock clock) {
        this.patientRefRepository = patientRefRepository;
        this.providerRefRepository = providerRefRepository;
        this.priorAuthEligibleCodeRepository = priorAuthEligibleCodeRepository;
        this.priorAuthRequestRepository = priorAuthRequestRepository;
        this.payerClient = payerClient;
        this.clock = clock;
    }

    @Transactional
    public PriorAuthRequestResponse request(AuthRequest authRequest){
        Optional<PatientRef> patientRef = patientRefRepository.findById(authRequest.patientId());
        if (patientRef.isEmpty()) {
            return new PriorAuthRequestResponse(
                    PriorAuthRequestStatus.PATIENT_NOT_FOUND
            );
        }

        Optional<ProviderRef> providerRef = providerRefRepository.findById(authRequest.providerId());
        if (providerRef.isEmpty()) {
            return new PriorAuthRequestResponse(
                    PriorAuthRequestStatus.PROVIDER_NOT_FOUND
            );
        }

        if (!priorAuthEligibleCodeRepository.existsById(
                new PriorAuthEligibleCodeId(authRequest.requestedCode(), authRequest.codeType())
        )) {
            return new PriorAuthRequestResponse(
                    PriorAuthRequestStatus.CODE_NOT_PA_ELIGIBLE
            );
        }

        List<PriorAuthRequest> matchingRequests = priorAuthRequestRepository.findByPatientIdAndRequestedCodeAndCodeType(
                authRequest.patientId(),
                authRequest.requestedCode(),
                authRequest.codeType()
        );
        Instant now = Instant.now(clock);
        boolean hasDuplicate = matchingRequests.stream().anyMatch(request ->
                request.getStatus() == RequestStatus.SUBMITTED
                        || request.getStatus() == RequestStatus.IN_REVIEW
                        || (request.getStatus() == RequestStatus.APPROVED
                            && (request.getExpiresAt() == null || request.getExpiresAt().isAfter(now))));
        if (hasDuplicate) {
            return new PriorAuthRequestResponse(
                    PriorAuthRequestStatus.DUPLICATE_ACTIVE_REQUEST
            );
        }

        List<CoverageItem> coverages = payerClient.getCoverages(authRequest.patientId());
        Integer currentYear = LocalDate.now(clock).getYear();
        Optional<CoverageItem> coverageItem = coverages.stream().filter(coverage -> coverage.startYear() <= currentYear && coverage.endYear() >= currentYear).findFirst();

        if(coverageItem.isEmpty()) {
            return new PriorAuthRequestResponse(
                    PriorAuthRequestStatus.PATIENT_NOT_COVERED
            );
        }

        UUID payerId = coverageItem.get().payerId();

        PriorAuthRequest priorAuthRequest = new PriorAuthRequest(
                UUID.randomUUID(),
                authRequest.patientId(),
                authRequest.providerId(),
                providerRef.get().getOrganizationId(),
                payerId,
                authRequest.requestedCode(),
                authRequest.codeType(),
                Instant.now(clock),
                authRequest.reason(),
                RequestStatus.SUBMITTED,
                null,
                TEMP_USER_ID
        );
        // TEMPORARY PA-29 synchronous bridge. Replace with Kafka in PA-35.
        AuthReviewResponse result = payerClient.submitRequest(new AuthReviewRequest(
                priorAuthRequest.getId(), priorAuthRequest.getPatientId(), priorAuthRequest.getProviderId(),
                priorAuthRequest.getOrganizationId(), priorAuthRequest.getPayerId(),
                priorAuthRequest.getRequestedCode(), priorAuthRequest.getCodeType(),
                priorAuthRequest.getReason(), priorAuthRequest.getSubmittedAt()));

        priorAuthRequest.recordPayerResult(result.status(), result.statusReason(),
                result.decidedAt(), result.expiresAt());

        priorAuthRequestRepository.save(priorAuthRequest);
        return new PriorAuthRequestResponse(
                PriorAuthRequestStatus.SUBMITTED, priorAuthRequest.getId(), null
        );
    }

    public PriorAuthRequestItem getRequestItem(UUID requestId) {
        Optional<PriorAuthRequest> request = this.priorAuthRequestRepository.findById(requestId);

        if (request.isEmpty()) {
            return null;
        }

        PriorAuthRequest priorAuthRequest = request.get();

        return new PriorAuthRequestItem(
                priorAuthRequest.getId(),
                priorAuthRequest.getPatientId(),
                priorAuthRequest.getProviderId(),
                priorAuthRequest.getOrganizationId(),
                priorAuthRequest.getRequestedCode(),
                priorAuthRequest.getCodeType(),
                priorAuthRequest.getReason(),
                priorAuthRequest.getStatus(),
                priorAuthRequest.getStatusReason(),
                priorAuthRequest.getSubmittedAt(),
                priorAuthRequest.getDecidedAt(),
                priorAuthRequest.getExpiresAt(),
                priorAuthRequest.getAppealOf(),
                priorAuthRequest.getSubmittedBy()
        );
    }

    public List<PriorAuthRequestItem> getRequestItems(UUID patientId, RequestStatus status, Pageable pageable) {
        List<PriorAuthRequest> requests = new ArrayList<>();
        if(patientId == null && status == null){
            requests = this.priorAuthRequestRepository.findAll(pageable);
        } else if(patientId == null) {
            requests = this.priorAuthRequestRepository.findByStatus(status, pageable);
        } else if(status == null) {
            requests = this.priorAuthRequestRepository.findByPatientId(patientId, pageable);
        } else {
            requests = this.priorAuthRequestRepository.findByPatientIdAndStatus(patientId, status, pageable);
        }

        List<PriorAuthRequestItem> requestItems = requests.stream().map(item -> new PriorAuthRequestItem(
                item.getId(),
                item.getPatientId(),
                item.getProviderId(),
                item.getOrganizationId(),
                item.getRequestedCode(),
                item.getCodeType(),
                item.getReason(),
                item.getStatus(),
                item.getStatusReason(),
                item.getSubmittedAt(),
                item.getDecidedAt(),
                item.getExpiresAt(),
                item.getAppealOf(),
                item.getSubmittedBy()
        )).toList();

        return requestItems;
    }
}
