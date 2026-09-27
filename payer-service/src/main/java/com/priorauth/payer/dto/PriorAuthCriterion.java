package com.priorauth.payer.dto;

import com.priorauth.payer.domain.CodeType;

public record PriorAuthCriterion(
        String code,
        CodeType codeType,
        String criterionDescription,
        String requiredConditionCode,
        Boolean autoApproveIfMet,
        Integer defaultValidityDays
) {
}
