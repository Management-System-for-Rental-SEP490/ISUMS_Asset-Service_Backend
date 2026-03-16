package com.isums.assetservice.domains.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedResponse<T> {
    List<T> items;
    String nextCursor;
    boolean hasMore;
}