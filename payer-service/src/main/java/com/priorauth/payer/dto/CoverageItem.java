package com.priorauth.payer.dto;

import java.util.UUID;

public record CoverageItem(
        UUID payerId,
        Integer startYear,
        Integer endYear,
        String ownership
) {
}
