package com.isums.assetservice.domains.dtos;

import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;

import java.util.List;

public record BatchUpdateResponse(
        int total,
        int success,
        List<AssetItemDto> assets
) {
}
