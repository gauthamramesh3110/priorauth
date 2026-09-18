package com.priorauth.payer.repository;

import com.priorauth.payer.domain.PriorAuthEligibleCode;
import com.priorauth.payer.domain.PriorAuthEligibleCodeId;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface PriorAuthEligibleCodeRepository extends Repository<PriorAuthEligibleCode, PriorAuthEligibleCodeId> {
    boolean existsById(PriorAuthEligibleCodeId id);

    Optional<PriorAuthEligibleCode> findById(PriorAuthEligibleCodeId id);
}
