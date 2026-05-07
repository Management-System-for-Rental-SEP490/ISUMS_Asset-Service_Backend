package com.isums.assetservice.domains.dtos;

public record IotSafetyThresholdDto(
        String metric,
        String displayName,
        String unit,
        Double comfortMin,
        Double comfortMax,
        Double warningMin,
        Double warningMax,
        Double criticalThreshold,
        Boolean criticalIsHigh,
        String standardRef
) {}
