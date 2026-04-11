package com.isums.assetservice.domains.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.isums.assetservice.domains.enums.PowerCutReason;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AreaPowerStateResponse(
        UUID houseId,
        UUID areaId,
        boolean powered,
        PowerCutReason cutReason,
        String message,
        Instant changedAt
) {}