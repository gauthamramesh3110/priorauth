package com.priorauth.provider_service.service;

import com.priorauth.provider_service.dto.ConditionItem;
import com.priorauth.provider_service.dto.EligibleCode;
import com.priorauth.provider_service.dto.PatientItem;
import com.priorauth.provider_service.repository.PatientConditionRepository;
import com.priorauth.provider_service.repository.PatientRefRepository;
import com.priorauth.provider_service.repository.PriorAuthEligibleCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReferenceDataService {
    private final PatientRefRepository patients;
    private final PatientConditionRepository conditions;
    private final PriorAuthEligibleCodeRepository eligibleCodes;

    public ReferenceDataService(PatientRefRepository patients, PatientConditionRepository conditions,
                                PriorAuthEligibleCodeRepository eligibleCodes) {
        this.patients = patients;
        this.conditions = conditions;
        this.eligibleCodes = eligibleCodes;
    }

    public Optional<PatientItem> getPatient(UUID patientId) {
        return patients.findPatientItemById(patientId);
    }

    public Optional<List<ConditionItem>> getConditions(UUID patientId) {
        if (patients.findPatientItemById(patientId).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(conditions.findByPatientIdOrderByOnsetDateDescIdAsc(patientId).stream()
                .map(condition -> new ConditionItem(
                        condition.getPatientId(), condition.getCode(), condition.getDescription(),
                        condition.getOnsetDate(), condition.getResolvedDate()))
                .toList());
    }

    public List<EligibleCode> getEligibleCodes() {
        return eligibleCodes.findAllByOrderByIdCodeTypeAscIdCodeAsc().stream()
                .map(code -> new EligibleCode(code.getId().getCode(), code.getId().getCodeType(),
                        code.getDescription()))
                .toList();
    }
}
