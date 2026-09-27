package com.priorauth.payer.repository;

import com.priorauth.payer.domain.PriorAuthReview;
import com.priorauth.payer.domain.ReviewTier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriorAuthReviewRepository extends Repository<PriorAuthReview, UUID> {
    Optional<PriorAuthReview> findByRequestId(UUID requestId);

    List<PriorAuthReview> findByReviewerIdAndReviewTierAndExpiresAtBefore(UUID reviewerId, ReviewTier reviewTier, Instant expiresAtBefore,  Pageable pageable);

    PriorAuthReview save(PriorAuthReview review);
}
