package com.priorauth.provider_service.repository;

import com.priorauth.provider_service.domain.ProviderRef;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface ProviderRefRepository extends Repository<ProviderRef, UUID> {
    Optional<ProviderRef> findById(UUID id);
}
