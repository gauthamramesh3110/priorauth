package com.priorauth.payer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Entity
public class PriorAuthEligibleCode {
    @EmbeddedId
    private PriorAuthEligibleCodeId id;
    @Column(nullable = false, length = 256)
    private String description;
    @Column(precision = 12, scale = 2)
    private BigDecimal typicalCost;
}
