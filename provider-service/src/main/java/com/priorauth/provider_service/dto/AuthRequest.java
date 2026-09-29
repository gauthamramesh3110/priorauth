package com.priorauth.provider_service.dto;

import com.priorauth.provider_service.domain.CodeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AuthRequest(
        @NotNull UUID patientId,
        @NotNull UUID providerId,
        @NotBlank String requestedCode,
        @NotNull @Valid CodeType codeType,
        String reason
) {
}
