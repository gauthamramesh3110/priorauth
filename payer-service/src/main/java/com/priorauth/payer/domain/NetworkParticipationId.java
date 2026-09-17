package com.priorauth.payer.domain;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@NoArgsConstructor
@EqualsAndHashCode
@Getter
@Embeddable
public class NetworkParticipationId {
    public NetworkParticipationId(UUID payerId, UUID organizationId) {
        this.payerId = Objects.requireNonNull(payerId, "payerId is required");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
    }

    private UUID payerId;
    private UUID organizationId;
}
