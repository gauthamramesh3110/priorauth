package com.priorauth.provider_service.controller;

import com.priorauth.provider_service.domain.RequestStatus;
import com.priorauth.provider_service.dto.AuthRequest;
import com.priorauth.provider_service.dto.PriorAuthRequestItem;
import com.priorauth.provider_service.dto.PriorAuthRequestResponse;
import com.priorauth.provider_service.dto.PriorAuthRequestStatus;
import com.priorauth.provider_service.service.PriorAuthRequestService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ProviderController {

    private final PriorAuthRequestService priorAuthRequestService;

    public ProviderController(PriorAuthRequestService priorAuthRequestService) {
        this.priorAuthRequestService = priorAuthRequestService;
    }

    @PostMapping("/requests")
    public ResponseEntity<PriorAuthRequestResponse> request(
            @Valid @RequestBody AuthRequest authRequest
            ) {
        PriorAuthRequestResponse response = this.priorAuthRequestService.request(authRequest);

        if(!PriorAuthRequestStatus.SUBMITTED.equals(response.status())){
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.created(java.net.URI.create("/api/v1/requests/" + response.requestId())).body(response);
    }

    @GetMapping("/requests/{requestId}")
    public ResponseEntity<PriorAuthRequestItem> getRequestById(
            @PathVariable(name = "requestId") UUID requestId
    ) {
        PriorAuthRequestItem requestItem = this.priorAuthRequestService.getRequestItem(requestId);
        if(requestItem == null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(requestItem);
    }

    @GetMapping("/requests")
    public ResponseEntity<List<PriorAuthRequestItem>> getRequests(
            @RequestParam(name = "patientId") UUID patientId,
            @RequestParam(name = "status") RequestStatus requestStatus,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(this.priorAuthRequestService.getRequestItems(patientId, requestStatus, pageable));
    }


}
