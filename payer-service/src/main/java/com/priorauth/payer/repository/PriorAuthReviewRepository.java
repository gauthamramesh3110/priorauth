package com.priorauth.payer.repository;

import com.priorauth.payer.domain.PriorAuthReview;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface PriorAuthReviewRepository extends Repository<PriorAuthReview, UUID> {
    Optional<PriorAuthReview> findByRequestId(UUID requestId);
}
