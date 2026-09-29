package com.priorauth.payer.service;

import com.priorauth.payer.domain.IntakeResult;
import com.priorauth.payer.domain.PriorAuthReview;
import com.priorauth.payer.repository.PriorAuthReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Domain orchestration used by the temporary PA-29 transport. */
@Service
public class RequestSubmissionService {
    private final PriorAuthReviewRepository reviews;
    private final IntakeValidationService intake;
    private final ClinicalReviewService clinical;

    public RequestSubmissionService(PriorAuthReviewRepository reviews, IntakeValidationService intake,
                                    ClinicalReviewService clinical) {
        this.reviews = reviews;
        this.intake = intake;
        this.clinical = clinical;
    }

    @Transactional
    public PriorAuthReview submit(PriorAuthReview review) {
        // Re-delivery must not re-evaluate an existing request or extend its approval.
        var existing = reviews.findByRequestId(review.getRequestId());
        if (existing.isPresent()) {
            return existing.get();
        }
        var outcome = intake.validate(review.getPatientId(), review.getPayerId(), review.getProviderId(),
                review.getRequestedCode(), review.getCodeType());
        review.recordIntake(outcome);
        if (outcome.intakeResult() == IntakeResult.REJECTED) {
            reviews.save(review);
        } else {
            clinical.review(review);
        }
        return review;
    }
}
