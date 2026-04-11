package com.isums.assetservice.domains.dtos;

import java.time.Instant;
import java.util.UUID;

public record AssetEventImageDto(
        UUID id,
        String url,
        Instant createdAt
) {
}
