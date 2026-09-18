package com.priorauth.payer.repository;

import com.priorauth.payer.domain.PatientCondition;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface PatientConditionRepository extends Repository<PatientCondition, Long> {
    List<PatientCondition> findByPatientIdAndCode(UUID patientId, String code);
}
