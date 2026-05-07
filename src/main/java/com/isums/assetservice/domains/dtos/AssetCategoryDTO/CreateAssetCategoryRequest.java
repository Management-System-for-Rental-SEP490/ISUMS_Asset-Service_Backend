package com.isums.assetservice.domains.dtos.AssetCategoryDTO;

import java.util.Map;

public record CreateAssetCategoryRequest(
        Map<String, String> name,
        int compensationPercent,
        Map<String, String> description
) {
}

