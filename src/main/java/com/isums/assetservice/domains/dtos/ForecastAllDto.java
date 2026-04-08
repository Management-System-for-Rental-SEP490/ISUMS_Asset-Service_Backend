package com.isums.assetservice.domains.dtos;

import java.util.Map;
import java.util.UUID;

public record ForecastAllDto(
        UUID houseId,
        String month,
        ForecastMetricDto electricity,
        ForecastMetricDto water
) {
    public record ForecastMetricDto(
            ForecastScopeDto house,
            Map<String, ForecastScopeDto> areas
    ) {}
}