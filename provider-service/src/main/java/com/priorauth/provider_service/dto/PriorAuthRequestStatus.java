package com.priorauth.provider_service.dto;

public enum PriorAuthRequestStatus {
    PATIENT_NOT_FOUND,
    PROVIDER_NOT_FOUND,
    CODE_NOT_PA_ELIGIBLE,
    PATIENT_NOT_COVERED,
    DUPLICATE_ACTIVE_REQUEST,
    SUBMITTED
}
