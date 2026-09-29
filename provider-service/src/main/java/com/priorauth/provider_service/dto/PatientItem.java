package com.priorauth.provider_service.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PatientItem(UUID id, String firstName, String lastName, LocalDate birthdate) {
}
