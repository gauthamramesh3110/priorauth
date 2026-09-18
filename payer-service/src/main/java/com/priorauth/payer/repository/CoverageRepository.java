package com.priorauth.payer.repository;

import com.priorauth.payer.domain.Coverage;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CoverageRepository extends Repository<Coverage, Long> {

    @Query(value = """
                SELECT c FROM Coverage c
                    WHERE c.patientId = :patientId
                      AND c.payerId = :payerId
                      AND c.startYear <= :year
                      AND c.endYear >= :year
            """)
    List<Coverage> findCoveragesYear(@Param("patientId") UUID patientId, @Param("payerId") UUID payerId, @Param("year") Integer year);
}
