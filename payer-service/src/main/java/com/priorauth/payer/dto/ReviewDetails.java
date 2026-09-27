package com.priorauth.payer.dto;

import com.priorauth.payer.domain.Decision;

import java.util.List;

public record ReviewDetails(
        ReviewItem reviewItem,
        PriorAuthCriterion priorAuthCriterion,
        List<ConditionItem> conditionItems,
        List<CoverageItem> coverageItems,
        Decision parentDecision,
        String parentDecisionReason
) {
}
