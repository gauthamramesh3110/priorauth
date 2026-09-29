package com.priorauth.provider_service.repository;

import com.priorauth.provider_service.domain.PatientCondition;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface PatientConditionRepository extends Repository<PatientCondition, Long> {
    List<PatientCondition> findByPatientIdOrderByOnsetDateDescIdAsc(UUID patientId);
}
