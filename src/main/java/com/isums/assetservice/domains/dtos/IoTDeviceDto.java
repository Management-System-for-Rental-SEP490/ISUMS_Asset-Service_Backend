package com.isums.assetservice.domains.dtos;

import com.isums.assetservice.domains.enums.DetectionType;

import java.util.UUID;

public record IoTDeviceDto(
        UUID id,
        String thing,
        String serialNumber,
        UUID assetId,
        UUID houseId,
        UUID categoryId,
        String categoryCode,
        DetectionType detectionType
) {
}
