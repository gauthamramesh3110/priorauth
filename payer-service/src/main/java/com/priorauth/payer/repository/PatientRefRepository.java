package com.priorauth.payer.repository;

import com.priorauth.payer.domain.PatientRef;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface PatientRefRepository extends Repository<PatientRef, UUID> {
    Optional<PatientRef> findById(UUID id);
}
