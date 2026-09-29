package com.priorauth.provider_service.httpclient;

import com.priorauth.provider_service.dto.CoverageItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
public class PayerClient {
    private final RestClient restClient;

    public PayerClient(@Value("${payer.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public List<CoverageItem> getCoverages(UUID patientId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/coverage")
                        .queryParam("patientId", patientId)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
