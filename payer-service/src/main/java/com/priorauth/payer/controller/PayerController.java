package com.priorauth.payer.controller;

import java.util.List;
import java.util.UUID;

import com.priorauth.payer.domain.PriorAuthReview;
import com.priorauth.payer.dto.ReviewQueue;
import com.priorauth.payer.service.ClinicalReviewService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
