package com.priorauth.payer.dto;

import java.util.List;

public record ReviewQueue(
        Integer page,
        Integer size,
        Integer totalElements,
        List<ReviewItem> content
) {
}
