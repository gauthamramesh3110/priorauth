package com.priorauth.provider_service.repository;

import com.priorauth.provider_service.domain.CodeType;
import com.priorauth.provider_service.domain.PriorAuthRequest;
import com.priorauth.provider_service.domain.RequestStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriorAuthRequestRepository extends Repository<PriorAuthRequest, UUID> {
    List<PriorAuthRequest> findByPatientIdAndRequestedCodeAndCodeType(UUID patientId, String requestedCode, CodeType codeType);

    PriorAuthRequest save(PriorAuthRequest priorAuthRequest);

    Optional<PriorAuthRequest> findById(UUID id);

    List<PriorAuthRequest> findAll(Pageable pageable);
    List<PriorAuthRequest> findByPatientId(UUID patientId, Pageable pageable);
    List<PriorAuthRequest> findByStatus(RequestStatus status, Pageable pageable);
    List<PriorAuthRequest> findByPatientIdAndStatus(UUID patientId, RequestStatus status, Pageable  pageable);
}
