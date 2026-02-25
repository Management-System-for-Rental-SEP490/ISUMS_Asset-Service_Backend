package com.isums.assetservice.domains.dtos.AssetItemDTO;

import java.util.UUID;

public record UpdateHouseRequest(
        UUID newHouseId
) {
}
