package com.priorauth.provider_service.dto;

import com.priorauth.provider_service.domain.CodeType;

public record EligibleCode(String code, CodeType codeType, String description) {
}
