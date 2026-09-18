package com.priorauth.payer.repository;

import com.priorauth.payer.domain.CodeType;
import com.priorauth.payer.domain.PriorAuthCriteria;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface PriorAuthCriteriaRepository extends Repository<PriorAuthCriteria, Long> {
    Optional<PriorAuthCriteria> findByCodeAndCodeType(String code, CodeType codeType);
}
