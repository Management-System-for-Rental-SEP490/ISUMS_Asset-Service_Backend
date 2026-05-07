package com.isums.assetservice.domains.dtos;

public record IotSafetyBandDto(
        String code,
        String label,
        String description,
        Integer scoreMax,
        String severity
) {}
