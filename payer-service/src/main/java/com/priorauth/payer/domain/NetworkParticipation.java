package com.priorauth.payer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NetworkParticipation {
    @EmbeddedId
    private NetworkParticipationId id;
    @Column(nullable = false)
    private Boolean inNetwork;
    @Column(nullable = false)
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    public NetworkParticipation(NetworkParticipationId id, Boolean inNetwork, LocalDate effectiveFrom, LocalDate effectiveTo) {
        this.id =  id;
        this.inNetwork = inNetwork;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }
}
