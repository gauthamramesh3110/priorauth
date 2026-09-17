package com.priorauth.payer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Entity
public class PatientCondition {
    @Id
    private Long id;

    @Column(nullable = false)
    private UUID patientId;

    private UUID encounterId;

    @Column(length = 32, nullable = false)
    private String code;

    @Column(length = 256)
    private String description;

    @Column(nullable = false)
    private LocalDate onsetDate;

    private LocalDate resolvedDate;
}
