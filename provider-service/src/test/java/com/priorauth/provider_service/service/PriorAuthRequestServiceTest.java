package com.priorauth.provider_service.service;

import com.priorauth.provider_service.domain.CodeType;
import com.priorauth.provider_service.domain.PriorAuthRequest;
import com.priorauth.provider_service.domain.RequestStatus;
import com.priorauth.provider_service.dto.PriorAuthRequestItem;
import com.priorauth.provider_service.httpclient.PayerClient;
import com.priorauth.provider_service.repository.PatientRefRepository;
import com.priorauth.provider_service.repository.PriorAuthEligibleCodeRepository;
import com.priorauth.provider_service.repository.PriorAuthRequestRepository;
import com.priorauth.provider_service.repository.ProviderRefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** PA-28 read-service behavior; repositories and the payer client are mocked. */
class PriorAuthRequestServiceTest {
    private static final UUID REQUEST = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PATIENT = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID PROVIDER = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID ORGANIZATION = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID PAYER = UUID.fromString("50000000-0000-0000-0000-000000000005");
    private static final UUID PARENT = UUID.fromString("60000000-0000-0000-0000-000000000006");
    private static final Instant SUBMITTED = Instant.parse("2026-09-20T12:00:00Z");
    private static final Instant DECIDED = Instant.parse("2026-09-21T12:00:00Z");
    private static final Instant EXPIRES = Instant.parse("2026-10-21T12:00:00Z");

    private final PriorAuthRequestRepository requests = mock(PriorAuthRequestRepository.class);
    private final PatientRefRepository patients = mock(PatientRefRepository.class);
    private final ProviderRefRepository providers = mock(ProviderRefRepository.class);
    private final PriorAuthEligibleCodeRepository codes = mock(PriorAuthEligibleCodeRepository.class);
    private final PayerClient payer = mock(PayerClient.class);
    private PriorAuthRequestService service;

    @BeforeEach
    void setUp() {
        service = new PriorAuthRequestService(patients, providers, codes, requests, payer,
                Clock.fixed(SUBMITTED, ZoneOffset.UTC));
    }

    @ParameterizedTest
    @CsvSource({"false,false,0,20", "true,false,1,1", "false,true,2,5", "true,true,3,10"})
    void selectsExactlyOneFilterQueryAndPreservesPageAndResultOrder(
            boolean filterPatient, boolean filterStatus, int page, int size) {
        UUID patient = filterPatient ? PATIENT : null;
        RequestStatus status = filterStatus ? RequestStatus.APPROVED : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        PriorAuthRequest first = request(REQUEST, RequestStatus.APPROVED, true);
        PriorAuthRequest second = request(UUID.fromString("70000000-0000-0000-0000-000000000007"),
                RequestStatus.APPROVED, false);
        // A size-one page can only contain one row.
        List<PriorAuthRequest> rows = size == 1 ? List.of(first) : List.of(first, second);
        stubListQuery(patient, status, pageable, rows);

        List<PriorAuthRequestItem> result = service.getRequestItems(patient, status, pageable);

        assertEquals(size == 1
                ? List.of(expected(REQUEST, RequestStatus.APPROVED, true))
                : List.of(expected(REQUEST, RequestStatus.APPROVED, true),
                          expected(second.getId(), RequestStatus.APPROVED, false)), result);
        verifyListQuery(patient, status, pageable);
        verifyNoMoreInteractions(requests);
        verifyNoInteractions(patients, providers, codes, payer);
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void emptyOrOutOfRangePageReturnsEmptyList(boolean filterPatient, boolean filterStatus) {
        UUID patient = filterPatient ? PATIENT : null;
        RequestStatus status = filterStatus ? RequestStatus.DENIED : null;
        Pageable pageable = PageRequest.of(100, 20, Sort.by("id"));
        stubListQuery(patient, status, pageable, List.of());

        assertEquals(List.of(), service.getRequestItems(patient, status, pageable));

        verifyListQuery(patient, status, pageable);
        verifyNoMoreInteractions(requests);
        verifyNoInteractions(patients, providers, codes, payer);
    }

    @ParameterizedTest
    @EnumSource(RequestStatus.class)
    void detailPreservesRequestIdStatusAndEveryResponseField(RequestStatus status) {
        when(requests.findById(REQUEST)).thenReturn(Optional.of(request(REQUEST, status, true)));

        assertEquals(expected(REQUEST, status, true), service.getRequestItem(REQUEST));

        verify(requests).findById(REQUEST);
        verifyNoMoreInteractions(requests);
        verifyNoInteractions(patients, providers, codes, payer);
    }

    @Test
    void pendingDetailPreservesNullableFields() {
        when(requests.findById(REQUEST)).thenReturn(Optional.of(request(REQUEST, RequestStatus.SUBMITTED, false)));

        assertEquals(expected(REQUEST, RequestStatus.SUBMITTED, false), service.getRequestItem(REQUEST));
    }

    @Test
    void unknownRequestReturnsNullWithoutOtherLookups() {
        when(requests.findById(REQUEST)).thenReturn(Optional.empty());

        assertNull(service.getRequestItem(REQUEST));

        verify(requests).findById(REQUEST);
        verifyNoMoreInteractions(requests);
        verifyNoInteractions(patients, providers, codes, payer);
    }

    private void stubListQuery(UUID patient, RequestStatus status, Pageable pageable, List<PriorAuthRequest> rows) {
        if (patient == null && status == null) {
            when(requests.findAll(pageable)).thenReturn(rows);
        } else if (patient == null) {
            when(requests.findByStatus(status, pageable)).thenReturn(rows);
        } else if (status == null) {
            when(requests.findByPatientId(patient, pageable)).thenReturn(rows);
        } else {
            when(requests.findByPatientIdAndStatus(patient, status, pageable)).thenReturn(rows);
        }
    }

    private void verifyListQuery(UUID patient, RequestStatus status, Pageable pageable) {
        if (patient == null && status == null) {
            verify(requests).findAll(pageable);
        } else if (patient == null) {
            verify(requests).findByStatus(status, pageable);
        } else if (status == null) {
            verify(requests).findByPatientId(patient, pageable);
        } else {
            verify(requests).findByPatientIdAndStatus(patient, status, pageable);
        }
    }

    private PriorAuthRequest request(UUID id, RequestStatus status, boolean withOptionalFields) {
        PriorAuthRequest request = new PriorAuthRequest(id, PATIENT, PROVIDER, ORGANIZATION, PAYER,
                "MRI", CodeType.IMAGING, SUBMITTED,
                withOptionalFields ? "Provider narrative\nSecond line" : null,
                status, withOptionalFields ? "Payer decision reason" : null, "clinician-subject");
        if (withOptionalFields) {
            ReflectionTestUtils.setField(request, "decidedAt", DECIDED);
            ReflectionTestUtils.setField(request, "expiresAt", EXPIRES);
            ReflectionTestUtils.setField(request, "appealOf", PARENT);
        }
        return request;
    }

    private PriorAuthRequestItem expected(UUID id, RequestStatus status, boolean withOptionalFields) {
        return new PriorAuthRequestItem(id, PATIENT, PROVIDER, ORGANIZATION, "MRI", CodeType.IMAGING,
                withOptionalFields ? "Provider narrative\nSecond line" : null,
                status, withOptionalFields ? "Payer decision reason" : null, SUBMITTED,
                withOptionalFields ? DECIDED : null, withOptionalFields ? EXPIRES : null,
                withOptionalFields ? PARENT : null, "clinician-subject");
    }
}
