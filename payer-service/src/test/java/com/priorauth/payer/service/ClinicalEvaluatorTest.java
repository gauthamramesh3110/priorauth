package com.priorauth.payer.service;

import com.priorauth.payer.domain.CodeType;
import com.priorauth.payer.domain.CriteriaEvaluationOutcome;
import com.priorauth.payer.domain.EscalationReason;
import com.priorauth.payer.domain.EvaluationResult;
import com.priorauth.payer.domain.PatientCondition;
import com.priorauth.payer.domain.PriorAuthCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClinicalEvaluatorTest {
    private static final LocalDate SUBMISSION_DATE = LocalDate.of(2026, 9, 26);
    private static final String REQUIRED_CODE = "44054006";
    private final ClinicalEvaluator evaluator = new ClinicalEvaluator();

    @Test
    void escalatesWhenCriterionIsMissingEvenWithQualifyingCondition() {
        assertEquals(escalated(EscalationReason.NO_CRITERION_DEFINED),
                evaluator.evaluate(Optional.empty(),
                        List.of(condition(REQUIRED_CODE, -1, null)), SUBMISSION_DATE));
    }

    @Test
    void escalatesWhenCriterionIsMissingAndConditionsAreEmpty() {
        assertEquals(escalated(EscalationReason.NO_CRITERION_DEFINED),
                evaluator.evaluate(Optional.empty(), List.of(), SUBMISSION_DATE));
    }

    @Test
    void escalatesWhenAutoApprovalIsDisabledEvenWithQualifyingCondition() {
        assertEquals(escalated(EscalationReason.ALWAYS_PHYSICIAN_REVIEW),
                evaluate(criterion(false), List.of(condition(REQUIRED_CODE, -1, null))));
    }

    @Test
    void disabledAutoApprovalTakesPrecedenceOverMissingCondition() {
        assertEquals(escalated(EscalationReason.ALWAYS_PHYSICIAN_REVIEW),
                evaluate(criterion(false), List.of()));
    }

    @Test
    void escalatesWhenConditionsAreEmpty() {
        assertEquals(escalated(EscalationReason.REQUIRED_CONDITION_ABSENT),
                evaluate(criterion(true), List.of()));
    }

    @Test
    void escalatesWhenOnlyAnUnrelatedConditionIsActive() {
        assertEquals(escalated(EscalationReason.REQUIRED_CONDITION_ABSENT),
                evaluate(criterion(true), List.of(condition("OTHER_CODE", -1, null))));
    }

    @ParameterizedTest(name = "onset offset={0}, resolution offset={1}: {2}")
    @CsvSource({
            "-2,     , AUTO_APPROVE",
            "-2,   -1, ESCALATE",
            "-2,    0, AUTO_APPROVE",
            "-2,    1, AUTO_APPROVE",
            " 0,     , ESCALATE",
            " 0,    0, ESCALATE",
            " 0,    1, ESCALATE",
            " 1,     , ESCALATE",
            " 1,    2, ESCALATE"
    })
    void evaluatesOnsetAndResolutionBoundaries(int onsetOffset, Integer resolutionOffset,
            EvaluationResult expectedResult) {
        CriteriaEvaluationOutcome expected = expectedResult == EvaluationResult.AUTO_APPROVE
                ? new CriteriaEvaluationOutcome(EvaluationResult.AUTO_APPROVE, null)
                : escalated(EscalationReason.REQUIRED_CONDITION_ABSENT);

        assertEquals(expected, evaluate(criterion(true),
                List.of(condition(REQUIRED_CODE, onsetOffset, resolutionOffset))));
    }

    @Test
    void comparesCodeValuesRatherThanStringIdentity() {
        // Separate objects with equal text reproduce values loaded independently from a database.
        String separatelyLoadedCode = new String(REQUIRED_CODE);
        assertEquals(new CriteriaEvaluationOutcome(EvaluationResult.AUTO_APPROVE, null),
                evaluate(criterion(true), List.of(condition(separatelyLoadedCode, -1, null))));
    }

    @Test
    void approvesWhenALaterConditionQualifies() {
        assertEquals(new CriteriaEvaluationOutcome(EvaluationResult.AUTO_APPROVE, null),
                evaluate(criterion(true), List.of(
                        condition("OTHER_CODE", -1, null),
                        condition(REQUIRED_CODE, -3, -1),
                        condition(REQUIRED_CODE, -1, null))));
    }

    @Test
    void approvesWhenFirstConditionQualifiesDespiteLaterNonmatchingConditions() {
        assertEquals(new CriteriaEvaluationOutcome(EvaluationResult.AUTO_APPROVE, null),
                evaluate(criterion(true), List.of(
                        condition(REQUIRED_CODE, -1, null),
                        condition("OTHER_CODE", -1, null))));
    }

    @Test
    void doesNotCombineCodeFromOneConditionWithDatesFromAnother() {
        assertEquals(escalated(EscalationReason.REQUIRED_CONDITION_ABSENT),
                evaluate(criterion(true), List.of(
                        condition(REQUIRED_CODE, -3, -1),
                        condition("OTHER_CODE", -1, null),
                        condition(REQUIRED_CODE, 1, null))));
    }

    private CriteriaEvaluationOutcome evaluate(PriorAuthCriteria criterion, List<PatientCondition> conditions) {
        return evaluator.evaluate(Optional.of(criterion), conditions, SUBMISSION_DATE);
    }

    private static CriteriaEvaluationOutcome escalated(EscalationReason reason) {
        return new CriteriaEvaluationOutcome(EvaluationResult.ESCALATE, reason);
    }

    private static PriorAuthCriteria criterion(boolean autoApprove) {
        PriorAuthCriteria criterion = new PriorAuthCriteria(
                1L, "TEST_PROCEDURE", CodeType.PROCEDURE, "Requires condition", autoApprove, 30);
        // Entities have no setters/constructors for these fields; reflection is confined to fixtures.
        ReflectionTestUtils.setField(criterion, "requiredConditionCode", REQUIRED_CODE);
        return criterion;
    }

    private static PatientCondition condition(String code, int onsetOffset, Integer resolutionOffset) {
        PatientCondition condition = new PatientCondition();
        ReflectionTestUtils.setField(condition, "code", code);
        ReflectionTestUtils.setField(condition, "onsetDate", SUBMISSION_DATE.plusDays(onsetOffset));
        ReflectionTestUtils.setField(condition, "resolvedDate",
                resolutionOffset == null ? null : SUBMISSION_DATE.plusDays(resolutionOffset));
        return condition;
    }
}
