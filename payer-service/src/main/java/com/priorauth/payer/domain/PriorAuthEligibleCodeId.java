package com.priorauth.payer.domain;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@EqualsAndHashCode
@NoArgsConstructor
@Getter
@Embeddable
public class PriorAuthEligibleCodeId {
    @Column(length = 32, nullable = false)
    private String code;

    @Column(length = 16, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private CodeType codeType;

    public PriorAuthEligibleCodeId(String code, CodeType codeType) {
        this.code = Objects.requireNonNull(code, "code is required");
        this.codeType = Objects.requireNonNull(codeType, "codeType is required");
    }
}
