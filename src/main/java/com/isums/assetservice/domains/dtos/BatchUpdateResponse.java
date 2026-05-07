package com.isums.assetservice.domains.dtos;

import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BatchUpdateResponse(
        int total,
        int success,
        List<EventResult> events
) {
    public record EventResult(
            UUID assetId,
            UUID eventId
    ) {}
}