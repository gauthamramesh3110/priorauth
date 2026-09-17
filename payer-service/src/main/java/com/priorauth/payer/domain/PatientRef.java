package com.priorauth.payer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Entity
public class PatientRef {
    @Id
    private UUID id;

    @Column(length = 64, nullable = false)
    private String firstName;

    @Column(length = 64, nullable = false)
    private String lastName;

    @Column(nullable = false)
    private LocalDate birthdate;

    @Column(length = 8)
    private String gender;

    @Column(length = 128)
    private String address;

    @Column(length = 64)
    private String city;

    @Column(length = 32)
    private String state;

    @Column(length = 16)
    private String zip;
}
