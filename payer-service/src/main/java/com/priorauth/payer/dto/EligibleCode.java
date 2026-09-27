package com.priorauth.payer.dto;

import com.priorauth.payer.domain.CodeType;

import java.math.BigDecimal;

public record EligibleCode(
        String code,
        CodeType codeType,
        String description,
        BigDecimal typicalCost
) {
}
