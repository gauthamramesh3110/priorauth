package com.priorauth.provider_service.controller;

import com.priorauth.provider_service.dto.ConditionItem;
import com.priorauth.provider_service.dto.EligibleCode;
import com.priorauth.provider_service.dto.PatientItem;
import com.priorauth.provider_service.service.ReferenceDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ReferenceDataController {
    private final ReferenceDataService referenceDataService;

    public ReferenceDataController(ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    @GetMapping("/patients/{id}")
    public ResponseEntity<PatientItem> getPatient(@PathVariable("id") UUID id) {
        return ResponseEntity.of(referenceDataService.getPatient(id));
    }

    @GetMapping("/patients/{id}/conditions")
    public ResponseEntity<List<ConditionItem>> getConditions(@PathVariable("id") UUID id) {
        return ResponseEntity.of(referenceDataService.getConditions(id));
    }

    @GetMapping("/eligible-codes")
    public ResponseEntity<List<EligibleCode>> getEligibleCodes() {
        return ResponseEntity.ok(referenceDataService.getEligibleCodes());
    }
}
