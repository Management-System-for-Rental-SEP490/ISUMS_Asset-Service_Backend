package com.isums.assetservice.domains.dtos;

import java.time.Instant;
import java.util.UUID;

public record IotSafetyConfigVersionDto(
        UUID id,
        String version,
        IotSafetyConfigDto config,
        Instant effectiveFrom,
        Instant expiredAt,
        String notes,
        UUID createdBy,
        Instant createdAt,
        UUID expiredBy,
        boolean isActive
) {}
