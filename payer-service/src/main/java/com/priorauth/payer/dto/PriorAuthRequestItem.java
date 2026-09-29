package com.priorauth.payer.dto;
import com.priorauth.payer.domain.CodeType;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
/** Temporary PA-29 submission contract; remove with the REST bridge in PA-35. */
public record PriorAuthRequestItem(
        @NotNull UUID requestId, @NotNull UUID patientId, @NotNull UUID providerId,
        @NotNull UUID organizationId, @NotNull UUID payerId,
        @NotBlank @Size(max = 32) String requestedCode, @NotNull CodeType codeType,
        String reason, @NotNull Instant submittedAt
) {}
