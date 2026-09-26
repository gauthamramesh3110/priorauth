package com.priorauth.payer.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.priorauth.payer.domain.CodeType;
import com.priorauth.payer.domain.CriteriaEvaluationOutcome;
import com.priorauth.payer.domain.Decision;
import com.priorauth.payer.domain.EvaluationResult;
import com.priorauth.payer.domain.PatientCondition;
import com.priorauth.payer.domain.PriorAuthCriteria;
import com.priorauth.payer.domain.PriorAuthReview;
import com.priorauth.payer.repository.PatientConditionRepository;
import com.priorauth.payer.repository.PriorAuthCriteriaRepository;
import com.priorauth.payer.repository.PriorAuthReviewRepository;

@Service
public class ClinicalReviewService {
    private final PatientConditionRepository patientConditionRepository;
    private final PriorAuthCriteriaRepository priorAuthCriteriaRepository;
    private final PriorAuthReviewRepository priorAuthReviewRepository;
    private final DecisionService decisionService;
    private final ClinicalEvaluator evaluator;

    public ClinicalReviewService(PatientConditionRepository patientConditionRepository,
            PriorAuthCriteriaRepository priorAuthCriteriaRepository,
            PriorAuthReviewRepository priorAuthReviewRepository, DecisionService decisionService,
            ClinicalEvaluator evaluator) {
        this.patientConditionRepository = patientConditionRepository;
        this.priorAuthCriteriaRepository = priorAuthCriteriaRepository;
        this.priorAuthReviewRepository = priorAuthReviewRepository;
        this.decisionService = decisionService;
        this.evaluator = evaluator;
    }

    @Transactional
    public void review(PriorAuthReview review) {
        UUID patientId = review.getPatientId();
        String code = review.getRequestedCode();
        CodeType codeType = review.getCodeType();
        LocalDate submissionDate = review.getSubmittedAt().atZone(ZoneOffset.UTC).toLocalDate();

        Optional<PriorAuthCriteria> criterion = this.priorAuthCriteriaRepository.findByCodeAndCodeType(code, codeType);
        String requiredConditionCode = criterion.isPresent() ? criterion.get().getRequiredConditionCode() : null;

        List<PatientCondition> conditions = this.patientConditionRepository.findByPatientIdAndCode(patientId,
                requiredConditionCode);
        CriteriaEvaluationOutcome outcome = this.evaluator.evaluate(criterion, conditions, submissionDate);

        if (outcome.result().equals(EvaluationResult.AUTO_APPROVE)) {
            review.markForAutoApproval();
            this.decisionService.decide(review, Decision.APPROVED, "AUTO_APPROVED", criterion.get());
        } else {
            review.escalateToPhysician(outcome.reason());
            priorAuthReviewRepository.save(review);
        }
    }
}
