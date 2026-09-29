package com.priorauth.provider_service.service;

import com.priorauth.provider_service.domain.CodeType;
import com.priorauth.provider_service.domain.PatientCondition;
import com.priorauth.provider_service.domain.PriorAuthEligibleCode;
import com.priorauth.provider_service.domain.PriorAuthEligibleCodeId;
import com.priorauth.provider_service.dto.ConditionItem;
import com.priorauth.provider_service.dto.EligibleCode;
import com.priorauth.provider_service.dto.PatientItem;
import com.priorauth.provider_service.repository.PatientConditionRepository;
import com.priorauth.provider_service.repository.PatientRefRepository;
import com.priorauth.provider_service.repository.PriorAuthEligibleCodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReferenceDataServiceTest {
    private static final UUID PATIENT = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private final PatientRefRepository patients = mock(PatientRefRepository.class);
    private final PatientConditionRepository conditions = mock(PatientConditionRepository.class);
    private final PriorAuthEligibleCodeRepository codes = mock(PriorAuthEligibleCodeRepository.class);
    private final ReferenceDataService service = new ReferenceDataService(patients, conditions, codes);

    @Test
    void returnsPatientDemographicsWithoutUsingSubmissionLock() {
        PatientItem patient = patient();
        when(patients.findPatientItemById(PATIENT)).thenReturn(Optional.of(patient));

        assertEquals(Optional.of(patient), service.getPatient(PATIENT));

        verify(patients).findPatientItemById(PATIENT);
        verifyNoMoreInteractions(patients);
        verifyNoInteractions(conditions, codes);
    }

    @Test
    void unknownPatientReturnsEmptyOptional() {
        when(patients.findPatientItemById(PATIENT)).thenReturn(Optional.empty());

        assertTrue(service.getPatient(PATIENT).isEmpty());

        verify(patients).findPatientItemById(PATIENT);
        verifyNoMoreInteractions(patients);
        verifyNoInteractions(conditions, codes);
    }

    @Test
    void unknownPatientDoesNotQueryConditionHistory() {
        when(patients.findPatientItemById(PATIENT)).thenReturn(Optional.empty());

        assertTrue(service.getConditions(PATIENT).isEmpty());

        verify(patients).findPatientItemById(PATIENT);
        verifyNoMoreInteractions(patients);
        verifyNoInteractions(conditions, codes);
    }

    @Test
    void knownPatientWithNoConditionsReturnsPresentEmptyList() {
        when(patients.findPatientItemById(PATIENT)).thenReturn(Optional.of(patient()));
        when(conditions.findByPatientIdOrderByOnsetDateDescIdAsc(PATIENT)).thenReturn(List.of());

        assertEquals(Optional.of(List.of()), service.getConditions(PATIENT));

        verify(conditions).findByPatientIdOrderByOnsetDateDescIdAsc(PATIENT);
        verifyNoMoreInteractions(conditions);
        verifyNoInteractions(codes);
    }

    @Test
    void returnsAllConditionEpisodesWithDatesNullsAndRepositoryOrderPreserved() {
        when(patients.findPatientItemById(PATIENT)).thenReturn(Optional.of(patient()));
        LocalDate recent = LocalDate.of(2026, 9, 20);
        LocalDate older = LocalDate.of(2020, 1, 1);
        LocalDate resolved = LocalDate.of(2021, 1, 1);
        when(conditions.findByPatientIdOrderByOnsetDateDescIdAsc(PATIENT)).thenReturn(List.of(
                condition("A", "Unresolved", recent, null),
                condition("B", null, recent, null),
                condition("A", "Earlier episode", older, resolved)));

        assertEquals(Optional.of(List.of(
                new ConditionItem(PATIENT, "A", "Unresolved", recent, null),
                new ConditionItem(PATIENT, "B", null, recent, null),
                new ConditionItem(PATIENT, "A", "Earlier episode", older, resolved))),
                service.getConditions(PATIENT));

        verify(patients).findPatientItemById(PATIENT);
        verify(conditions).findByPatientIdOrderByOnsetDateDescIdAsc(PATIENT);
        verifyNoMoreInteractions(patients, conditions);
        verifyNoInteractions(codes);
    }

    @Test
    void eligibleCodesPreserveCompositeKeysAndIncludeUltrasoundWithoutCriterion() {
        when(codes.findAllByOrderByIdCodeTypeAscIdCodeAsc()).thenReturn(List.of(
                code("US", CodeType.IMAGING, "Ultrasound"),
                code("SHARED", CodeType.MEDICATION, "Medication"),
                code("SHARED", CodeType.PROCEDURE, "Procedure")));

        assertEquals(List.of(
                new EligibleCode("US", CodeType.IMAGING, "Ultrasound"),
                new EligibleCode("SHARED", CodeType.MEDICATION, "Medication"),
                new EligibleCode("SHARED", CodeType.PROCEDURE, "Procedure")), service.getEligibleCodes());

        verify(codes).findAllByOrderByIdCodeTypeAscIdCodeAsc();
        verifyNoMoreInteractions(codes);
        verifyNoInteractions(patients, conditions);
    }

    @Test
    void noEligibleCodesReturnsEmptyList() {
        when(codes.findAllByOrderByIdCodeTypeAscIdCodeAsc()).thenReturn(List.of());

        assertEquals(List.of(), service.getEligibleCodes());

        verify(codes).findAllByOrderByIdCodeTypeAscIdCodeAsc();
        verifyNoMoreInteractions(codes);
        verifyNoInteractions(patients, conditions);
    }

    private PatientItem patient() {
        return new PatientItem(PATIENT, "Test", "Patient", LocalDate.of(1980, 1, 2));
    }

    private PatientCondition condition(String code, String description, LocalDate onset, LocalDate resolved) {
        PatientCondition condition = BeanUtils.instantiateClass(PatientCondition.class);
        ReflectionTestUtils.setField(condition, "patientId", PATIENT);
        ReflectionTestUtils.setField(condition, "code", code);
        ReflectionTestUtils.setField(condition, "description", description);
        ReflectionTestUtils.setField(condition, "onsetDate", onset);
        ReflectionTestUtils.setField(condition, "resolvedDate", resolved);
        return condition;
    }

    private PriorAuthEligibleCode code(String code, CodeType type, String description) {
        PriorAuthEligibleCode entity = new PriorAuthEligibleCode();
        ReflectionTestUtils.setField(entity, "id", new PriorAuthEligibleCodeId(code, type));
        ReflectionTestUtils.setField(entity, "description", description);
        return entity;
    }
}
