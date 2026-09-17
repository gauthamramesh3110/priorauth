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
public class PayerRef {
    @Id
    private UUID id;

    @Column(length = 128, nullable = false)
    private String name;

    @Column(length = 128)
    private String address;

    @Column(length = 64)
    private String city;

    @Column(length = 32)
    private String state;

    @Column(length = 16)
    private String zip;

    @Column(length = 24)
    private String phone;
}
