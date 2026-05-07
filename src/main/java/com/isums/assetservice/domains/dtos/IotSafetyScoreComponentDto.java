package com.isums.assetservice.domains.dtos;

public record IotSafetyScoreComponentDto(
        String metric,
        Double weight,
        String normalizationStrategy,
        Double normalizationParam
) {}
