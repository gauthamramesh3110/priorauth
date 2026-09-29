package com.priorauth.provider_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class PriorAuthEligibleCode {
    @EmbeddedId
    private PriorAuthEligibleCodeId id;
    @Column(nullable = false, length = 256)
    private String description;
}
