package com.priorauth.payer.repository;

import com.priorauth.payer.domain.PriorAuthReview;
import com.priorauth.payer.domain.ReviewTier;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriorAuthReviewRepository extends Repository<PriorAuthReview, UUID> {
    Optional<PriorAuthReview> findByRequestId(UUID requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PriorAuthReview r where r.requestId = :requestId")
    Optional<PriorAuthReview> findByRequestIdForUpdate(
            @Param("requestId") UUID requestId);

    List<PriorAuthReview> findByReviewerIdAndReviewTierAndExpiresAtBefore(UUID reviewerId, ReviewTier reviewTier, Instant expiresAtBefore,  Pageable pageable);

    PriorAuthReview save(PriorAuthReview review);
}
