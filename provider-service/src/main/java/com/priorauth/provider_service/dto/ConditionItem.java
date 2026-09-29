package com.priorauth.provider_service.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ConditionItem(
        UUID patientId,
        String code,
        String description,
        LocalDate onsetDate,
        LocalDate resolvedDate
) {
}
