package com.priorauth.payer.dto;

import com.priorauth.payer.domain.Decision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DecisionRequest(
        @NotNull Decision decision,
        @NotBlank @Size(max = 256) String decisionReason,
        @NotNull UUID reviewerId
) {
}
