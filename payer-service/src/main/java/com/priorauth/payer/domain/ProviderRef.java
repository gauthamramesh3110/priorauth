package com.priorauth.payer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
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

    @OneToMany(mappedBy = "providerRef", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PriorAuthReview> priorAuthReviews = new ArrayList<>();

    public ProviderRef(UUID id, UUID organizationId, String name, String specialty) {
        this.id = id;
        this.organizationId = organizationId;
        this.name = name;
        this.specialty = specialty;
    }
}
