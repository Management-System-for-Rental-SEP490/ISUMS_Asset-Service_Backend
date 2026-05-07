package com.isums.assetservice.domains.dtos;

public record ForecastTriggerResultDto(
        boolean ok,
        int created,
        int skipped,
        int total
) {
}
