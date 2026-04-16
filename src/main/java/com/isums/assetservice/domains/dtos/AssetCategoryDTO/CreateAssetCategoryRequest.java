package com.isums.assetservice.domains.dtos.AssetCategoryDTO;

import java.util.Map;

/**
 * {@code name} and {@code description} are multilingual maps,
 * e.g. {@code {"vi":"Bàn ghế","en":"Furniture"}}.
 * Supply at least one entry per field; "vi" is the default fallback.
 */
public record CreateAssetCategoryRequest(
        Map<String, String> name,
        int compensationPercent,
        Map<String, String> description
) {
}
