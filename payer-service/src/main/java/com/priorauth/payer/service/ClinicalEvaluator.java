package com.priorauth.payer.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.priorauth.payer.domain.CriteriaEvaluationOutcome;
import com.priorauth.payer.domain.EscalationReason;
import com.priorauth.payer.domain.EvaluationResult;
import com.priorauth.payer.domain.PatientCondition;
import com.priorauth.payer.domain.PriorAuthCriteria;

@Service
public class ClinicalEvaluator {

    CriteriaEvaluationOutcome evaluate(Optional<PriorAuthCriteria> criteria, List<PatientCondition> conditions,
            LocalDate submissionDate) {
        if (criteria.isEmpty()) {
            return new CriteriaEvaluationOutcome(EvaluationResult.ESCALATE, EscalationReason.NO_CRITERION_DEFINED);
        }

        if (!criteria.get().getAutoApproveIfMet()) {
            return new CriteriaEvaluationOutcome(EvaluationResult.ESCALATE, EscalationReason.ALWAYS_PHYSICIAN_REVIEW);
        }

        if (conditions.isEmpty()) {
            return new CriteriaEvaluationOutcome(EvaluationResult.ESCALATE, EscalationReason.REQUIRED_CONDITION_ABSENT);
        }

        for (PatientCondition condition : conditions) {
            LocalDate onsetDate = condition.getOnsetDate();
            LocalDate resolutionDate = condition.getResolvedDate();
            boolean isCorrectCodeForCriteria = condition.getCode().equals(criteria.get().getRequiredConditionCode());
            boolean isExistingCondition = onsetDate.isBefore(submissionDate)
                    && (resolutionDate == null || !resolutionDate.isBefore(submissionDate));

            if (isExistingCondition && isCorrectCodeForCriteria) {
                return new CriteriaEvaluationOutcome(EvaluationResult.AUTO_APPROVE, null);
            }
        }

        return new CriteriaEvaluationOutcome(EvaluationResult.ESCALATE, EscalationReason.REQUIRED_CONDITION_ABSENT);
    }
}
