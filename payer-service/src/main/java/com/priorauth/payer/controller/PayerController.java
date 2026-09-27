package com.priorauth.payer.controller;

import java.util.List;
import java.util.UUID;

import com.priorauth.payer.domain.CodeType;
import com.priorauth.payer.dto.*;
import com.priorauth.payer.service.ClinicalReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.priorauth.payer.domain.ReviewTier;

@RestController
@RequestMapping("/api/v1")
public class PayerController {

    private final ClinicalReviewService clinicalReviewService;
    public PayerController(ClinicalReviewService clinicalReviewService) {
        this.clinicalReviewService = clinicalReviewService;
    }

    @GetMapping("/review-queue")
    public ResponseEntity<ReviewQueue> getReviewQueue(
            @RequestParam(name = "tier", defaultValue = "PHYSICIAN") ReviewTier tier,
            @RequestParam("reviewerId") UUID reviewerId,
            @RequestParam("expiringWithinDays") Integer expiringWithinDays,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {
        return ResponseEntity.ok(clinicalReviewService.getReviewQueue(tier, reviewerId, expiringWithinDays, pageable));
    }

    @GetMapping("/review-queue/{requestId}")
    public ResponseEntity<ReviewDetails> getReviewItem(
        @PathVariable(name = "requestId") UUID requestId
    ) {
        ReviewDetails reviewDetails = clinicalReviewService.getReviewItem(requestId);

        if (reviewDetails == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(reviewDetails);
    }

    @PostMapping("/review-queue/{requestId}/claim")
    public ResponseEntity<ClaimReviewResponse> claimReviewItem(
            @PathVariable(name = "requestId") UUID requestId,
            @Valid @RequestBody ClaimReviewRequest claimReviewRequest
    ) {
        ClaimReviewResponse claimReviewResponse = clinicalReviewService.claimReviewItem(requestId, claimReviewRequest);

        if(claimReviewResponse == null) return ResponseEntity.notFound().build();

        if(claimReviewResponse.status().equals(ClaimReviewStatus.CLAIMED) || claimReviewResponse.status().equals(ClaimReviewStatus.ALREADY_OWNED)) {
            return ResponseEntity.ok(claimReviewResponse);
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(claimReviewResponse);
    }

    @PostMapping("/review-queue/{requestId}/decide")
    public ResponseEntity<DecisionResponse> decideReviewItem(
            @PathVariable(name = "requestId") UUID requestId,
            @Valid @RequestBody DecisionRequest decisionRequest
    ) {
        DecisionResponse response = this.clinicalReviewService.decideReviewItem(requestId, decisionRequest);
        if(response == null) return ResponseEntity.notFound().build();

        if(!response.status().equals(DecisionStatus.DECISION_UPDATED)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/coverage")
    public ResponseEntity<List<CoverageItem>> getCoverages(
            @RequestParam(name = "patientId") UUID patientId
    ) {
        return ResponseEntity.ok(clinicalReviewService.getCoverages(patientId));
    }

    @GetMapping("/eligible-codes")
    public ResponseEntity<List<EligibleCode>> getEligibleCodes() {
        return ResponseEntity.ok(clinicalReviewService.getEligibleCodes());
    }

    @GetMapping("/criteria/{codeType}/{code}")
    public ResponseEntity<PriorAuthCriterion> getPriorAuthCriterion(
            @PathVariable(name = "code") String code,
            @PathVariable(name = "codeType") CodeType codeType
    ) {
        PriorAuthCriterion criterion = clinicalReviewService.getPriorAuthCriteria(code, codeType);
        if (criterion == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(criterion);
    }
}
