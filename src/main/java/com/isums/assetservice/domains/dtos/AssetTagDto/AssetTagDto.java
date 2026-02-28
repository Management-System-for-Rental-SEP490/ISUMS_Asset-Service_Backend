package com.isums.assetservice.domains.dtos.AssetTagDto;

import com.isums.assetservice.domains.enums.TagType;

import java.time.Instant;
import java.util.UUID;

public record AssetTagDto(
        UUID id,
        String tagValue,
        TagType tagType,
        UUID assetId,
        UUID houseId,
        Instant activatedAt,
        Boolean isActive
) {
}
