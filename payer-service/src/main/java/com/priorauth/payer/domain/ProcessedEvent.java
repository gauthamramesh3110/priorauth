package com.priorauth.payer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
public class ProcessedEvent {
    public ProcessedEvent(UUID eventId, String topic, Instant consumedAt) {
        this.eventId = Objects.requireNonNull(eventId, "eventId is required");
        this.topic = Objects.requireNonNull(topic, "topic is required");
        this.consumedAt = Objects.requireNonNull(consumedAt, "consumedAt is required");
    }

    @Id
    private UUID eventId;

    @Column(length = 64, nullable = false)
    private String topic;

    @Column(nullable = false)
    private Instant consumedAt;
}
