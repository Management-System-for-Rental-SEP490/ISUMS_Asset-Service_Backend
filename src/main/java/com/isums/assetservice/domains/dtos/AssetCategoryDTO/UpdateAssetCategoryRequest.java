package com.isums.assetservice.domains.dtos.AssetCategoryDTO;

public record UpdateAssetCategoryRequest(
        String name,
        Integer compensationPercent,
        String description
) {}
