package com.priorauth.provider_service.repository;

import com.priorauth.provider_service.domain.PriorAuthEligibleCode;
import com.priorauth.provider_service.domain.PriorAuthEligibleCodeId;
import org.springframework.data.repository.Repository;

public interface PriorAuthEligibleCodeRepository extends Repository<PriorAuthEligibleCode, PriorAuthEligibleCodeId> {
    java.util.List<PriorAuthEligibleCode> findAllByOrderByIdCodeTypeAscIdCodeAsc();

    boolean existsById(PriorAuthEligibleCodeId id);
}
