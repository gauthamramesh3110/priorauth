package com.priorauth.payer.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.priorauth.payer.domain.*;
import com.priorauth.payer.dto.*;
import com.priorauth.payer.repository.CoverageRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.priorauth.payer.repository.PatientConditionRepository;
import com.priorauth.payer.repository.PriorAuthCriteriaRepository;
import com.priorauth.payer.repository.PriorAuthReviewRepository;

@Service
public class ClinicalReviewService {
    private final PatientConditionRepository patientConditionRepository;
    private final PriorAuthCriteriaRepository priorAuthCriteriaRepository;
    private final PriorAuthReviewRepository priorAuthReviewRepository;
    private final CoverageRepository coverageRepository;
    private final DecisionService decisionService;
    private final ClinicalEvaluator evaluator;
    private final Clock clock;

    public ClinicalReviewService(PatientConditionRepository patientConditionRepository,
            PriorAuthCriteriaRepository priorAuthCriteriaRepository,
            PriorAuthReviewRepository priorAuthReviewRepository, CoverageRepository coverageRepository, DecisionService decisionService,
            ClinicalEvaluator evaluator, Clock clock) {
        this.patientConditionRepository = patientConditionRepository;
        this.priorAuthCriteriaRepository = priorAuthCriteriaRepository;
        this.priorAuthReviewRepository = priorAuthReviewRepository;
        this.coverageRepository = coverageRepository;
        this.decisionService = decisionService;
        this.evaluator = evaluator;
        this.clock = clock;
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
            this.decisionService.decide(review, Decision.APPROVED, "AUTO_APPROVED", criterion.orElseThrow());
        } else {
            review.escalateToPhysician(outcome.reason());
            priorAuthReviewRepository.save(review);
        }
    }

    public ReviewQueue getReviewQueue(ReviewTier reviewTier, UUID reviewerId, Integer expiringWithinDays, Pageable pageable) {
        Instant expiresAt = Instant.now(clock).plus(expiringWithinDays, ChronoUnit.DAYS);
        List<PriorAuthReview> reviews = this.priorAuthReviewRepository.findByReviewerIdAndReviewTierAndExpiresAtBefore(reviewerId, reviewTier, expiresAt, pageable);
        List<ReviewItem> items = reviews.stream().map(this::generateReviewItem).toList();
        return new ReviewQueue(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                items.size(),
                items
        );
    }

    public ReviewDetails getReviewItem(UUID requestId) {
        Optional<PriorAuthReview> review = this.priorAuthReviewRepository.findByRequestId(requestId);

        if (review.isEmpty()) {
            return null;
        }

        ReviewItem reviewItem = generateReviewItem(review.get());

        UUID patientId = reviewItem.patientId();
        UUID payerId = review.get().getPayerId();
        String code = reviewItem.requestCode();
        CodeType codeType = reviewItem.codeType();
        Integer submissionYear = reviewItem.submittedAt().atZone(ZoneOffset.UTC).toLocalDate().getYear();
        PriorAuthCriterion priorAuthCriterion = null;
        List<ConditionItem> conditionItems = null;
        Decision parentDecision = null;
        String parentDecisionReason = null;

        List<Coverage> coverages = coverageRepository.findCoveragesYear(patientId, payerId, submissionYear);
        List<CoverageItem> coverageItems = coverages.stream().map(coverage -> new CoverageItem(
                coverage.getPayerId(),
                coverage.getStartYear(),
                coverage.getEndYear(),
                coverage.getOwnership()
        )).toList();



        if(reviewItem.isAppeal()) {
            Optional<PriorAuthReview> parentReview = this.priorAuthReviewRepository.findByRequestId(reviewItem.appealOf());
            if(parentReview.isPresent()) {
                parentDecision = parentReview.get().getDecision();
                parentDecisionReason = parentReview.get().getDecisionReason();
            }
        }

        Optional<PriorAuthCriteria> criterion = this.priorAuthCriteriaRepository.findByCodeAndCodeType(code, codeType);
        if (criterion.isPresent()) {
            PriorAuthCriteria priorAuthCriteria = criterion.get();
            priorAuthCriterion = new PriorAuthCriterion(
                    priorAuthCriteria.getCode(),
                    priorAuthCriteria.getCodeType(),
                    priorAuthCriteria.getCriterionDescription(),
                    priorAuthCriteria.getRequiredConditionCode(),
                    priorAuthCriteria.getAutoApproveIfMet(),
                    priorAuthCriteria.getDefaultValidityDays()
            );

            String requiredConditionCode = criterion.get().getRequiredConditionCode();
            List<PatientCondition> conditions = this.patientConditionRepository.findByPatientIdAndCode(patientId,
                    requiredConditionCode);


            conditionItems = conditions.stream().map(condition -> new ConditionItem(
                    condition.getPatientId(),
                    condition.getCode(),
                    condition.getDescription(),
                    condition.getOnsetDate(),
                    condition.getResolvedDate()
            )).toList();
        }


        return new ReviewDetails(
                reviewItem,
                priorAuthCriterion,
                conditionItems,
                coverageItems,
                parentDecision,
                parentDecisionReason
        );
    }

    private ReviewItem generateReviewItem(PriorAuthReview review) {
        return new ReviewItem(
                review.getRequestId(),
                review.getPatientRef().getFirstName() + review.getPatientRef().getLastName(),
                review.getPatientId(),
                review.getProviderRef().getName(),
                review.getProviderRef().getSpecialty(),
                review.getRequestedCode(),
                review.getCodeType(),
                review.getPriorAuthEligibleCode().getDescription(),
                review.getReason(),
                review.getReviewTier(),
                review.getReviewerId(),
                review.getSubmittedAt(),
                review.getEscalationReason(),
                review.getAppealOf() != null,
                review.getAppealOf()
        );
    }
}
