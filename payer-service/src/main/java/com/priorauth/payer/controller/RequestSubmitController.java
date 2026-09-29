package com.priorauth.payer.controller;

import com.priorauth.payer.domain.IntakeResult;
import com.priorauth.payer.domain.PriorAuthReview;
import com.priorauth.payer.dto.AuthReviewResponse;
import com.priorauth.payer.dto.PriorAuthRequestItem;
import com.priorauth.payer.service.RequestSubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** TEMPORARY PA-29 REST bridge. Delete in PA-35 when Kafka replaces submission. */
@RestController
@RequestMapping("/internal")
public class RequestSubmitController {
    private final RequestSubmissionService submissions;

    public RequestSubmitController(RequestSubmissionService submissions) {
        this.submissions = submissions;
    }

    @PostMapping("/submit-request")
    public ResponseEntity<AuthReviewResponse> submitRequest(@Valid @RequestBody PriorAuthRequestItem item) {
        PriorAuthReview review = submissions.submit(new PriorAuthReview(
                item.requestId(), item.patientId(), item.providerId(), item.organizationId(),
                item.payerId(), item.requestedCode(), item.codeType(), item.submittedAt(), item.reason()));
        String status;
        String reason = null;
        if (review.getIntakeResult() == IntakeResult.REJECTED) {
            status = "INTAKE_REJECTED";
            reason = review.getIntakeReason().name();
        } else if (review.getDecision() != null) {
            status = review.getDecision().name();
            reason = review.getDecisionReason();
        } else {
            status = "IN_REVIEW";
        }
        return ResponseEntity.ok(new AuthReviewResponse(
                review.getRequestId(), status, reason, review.getDecidedAt(), review.getExpiresAt()));
    }
}
