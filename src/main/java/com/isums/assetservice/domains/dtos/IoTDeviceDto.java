package com.isums.assetservice.domains.dtos;

import java.util.UUID;

public record IoTDeviceDto(
        UUID id,
        String thing,
        String serialNumber,
        UUID assetId,
        UUID houseId,
        UUID areaId,
        UUID categoryId,
        String categoryCode
) {
}
