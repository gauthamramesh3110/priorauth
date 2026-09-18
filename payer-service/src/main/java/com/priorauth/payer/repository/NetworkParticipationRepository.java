package com.priorauth.payer.repository;

import com.priorauth.payer.domain.NetworkParticipation;
import com.priorauth.payer.domain.NetworkParticipationId;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface NetworkParticipationRepository extends Repository<NetworkParticipation, NetworkParticipationId> {
    Optional<NetworkParticipation> findById(NetworkParticipationId id);
}
