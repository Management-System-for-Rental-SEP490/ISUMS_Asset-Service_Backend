package com.isums.assetservice.domains.dtos;

public record IotSafetyCapabilityGapDto(
        String metric,
        String displayName,
        String description,
        String requiredSensor,
        Long sensorPriceVndApprox,
        String standardRef
) {}
