package com.priorauth.provider_service.httpclient;

import com.priorauth.provider_service.dto.AuthReviewRequest;
import com.priorauth.provider_service.dto.AuthReviewResponse;
import com.priorauth.provider_service.dto.CoverageItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Component
public class PayerClient {
    private final RestClient restClient;

    public PayerClient(@Value("${payer.base-url}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public List<CoverageItem> getCoverages(UUID patientId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/coverage")
                        .queryParam("patientId", patientId).build())
                .retrieve().body(new ParameterizedTypeReference<>() {});
    }

    /** TEMPORARY PA-29 bridge. Remove in PA-35. */
    public AuthReviewResponse submitRequest(AuthReviewRequest request) {
        try {
            AuthReviewResponse response = restClient.post().uri("/internal/submit-request")
                    .contentType(MediaType.APPLICATION_JSON).body(request)
                    .retrieve().body(AuthReviewResponse.class);
            if (response == null || !request.requestId().equals(response.requestId()) || response.status() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid payer submission response");
            }
            return response;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payer submission failed", exception);
        }
    }
}
