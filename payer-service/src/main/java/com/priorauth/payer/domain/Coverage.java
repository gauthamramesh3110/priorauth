package com.priorauth.payer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coverage {
    public Coverage(
            Long id,
            UUID patientId,
            UUID payerId,
            Integer startYear,
            Integer endYear
    ) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.patientId = Objects.requireNonNull(patientId, "patientId is required");
        this.payerId = Objects.requireNonNull(payerId, "payerId is required");
        this.startYear = Objects.requireNonNull(startYear, "startYear is required");
        this.endYear = Objects.requireNonNull(endYear, "endYear is required");
    }

    @Id
    private Long id;
    @Column(nullable = false)
    private UUID patientId;
    @Column(nullable = false)
    private UUID payerId;
    @Column(nullable = false)
    private Integer startYear;
    @Column(nullable = false)
    private Integer endYear;
    @Column(length = 16)
    private String ownership;
}
