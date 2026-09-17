package com.priorauth.payer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Entity
public class ProviderRef {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(length = 128, nullable = false)
    private String name;

    @Column(length = 64)
    private String specialty;
}
