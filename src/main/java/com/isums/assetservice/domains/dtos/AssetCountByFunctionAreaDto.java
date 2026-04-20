package com.isums.assetservice.domains.dtos;

import java.util.UUID;

public record AssetCountByFunctionAreaDto(
        UUID functionAreaId,
        long assetCount
) {
}
