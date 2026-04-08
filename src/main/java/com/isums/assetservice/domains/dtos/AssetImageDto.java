package com.isums.assetservice.domains.dtos;

import java.time.Instant;
import java.util.UUID;

public record AssetImageDto(
        UUID id,
        String url,
        Instant createdAt
) {
}
