package com.priorauth.payer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@NoArgsConstructor
@Getter
@Entity
public class PriorAuthCriteria {
    public PriorAuthCriteria(
        Long id,
        String code,
        CodeType codeType,
        String criterionDescription,
        Boolean autoApproveIfMet,
        Integer defaultValidityDays
    ) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.code = Objects.requireNonNull(code, "code is required");
        this.codeType = Objects.requireNonNull(codeType, "codeType is required");
        this.criterionDescription = Objects.requireNonNull(criterionDescription, "criterionDescription is required");
        this.autoApproveIfMet = Objects.requireNonNull(autoApproveIfMet, "autoApproveIfMet is required");
        this.defaultValidityDays = Objects.requireNonNull(defaultValidityDays, "defaultValidityDays is required");
    }

    @Id
    private Long id;

    @Column(length = 32, nullable = false)
    private String code;

    @Column(length = 16, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private CodeType codeType;

    @Column(length = 512, nullable = false)
    private String criterionDescription;

    @Column(length = 32)
    private String requiredConditionCode;

    @Column(nullable = false)
    private Boolean autoApproveIfMet;

    @Column(nullable = false)
    private Integer defaultValidityDays;
}
