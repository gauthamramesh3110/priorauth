package com.priorauth.provider_service.repository;

import com.priorauth.provider_service.domain.PatientRef;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface PatientRefRepository extends Repository<PatientRef, UUID> {
    // DTO projection reads only provider-owned columns and does not lock submission rows.
    @org.springframework.data.jpa.repository.Query("""
            select new com.priorauth.provider_service.dto.PatientItem(
                p.id, p.firstName, p.lastName, p.birthdate)
            from PatientRef p where p.id = :id
            """)
    Optional<com.priorauth.provider_service.dto.PatientItem> findPatientItemById(
            @org.springframework.data.repository.query.Param("id") UUID id);

    // Serialize submissions for the same patient until the request transaction commits.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<PatientRef> findById(UUID id);
}
