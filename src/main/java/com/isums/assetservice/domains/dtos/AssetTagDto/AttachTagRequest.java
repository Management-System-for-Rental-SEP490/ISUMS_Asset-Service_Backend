package com.isums.assetservice.domains.dtos.AssetTagDto;

import com.isums.assetservice.domains.enums.TagType;

import java.util.UUID;

public record AttachTagRequest(
        UUID assetId,
        String tagValue,
        TagType tagType
) {
}
