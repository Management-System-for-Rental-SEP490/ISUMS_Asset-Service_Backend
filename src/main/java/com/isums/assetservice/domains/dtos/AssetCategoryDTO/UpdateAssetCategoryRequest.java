package com.isums.assetservice.domains.dtos.AssetCategoryDTO;

import java.util.Map;

/**
 * Null fields are ignored (existing value kept).
 * Non-null {@code name}/{@code description} maps fully replace existing translations.
 */
public record UpdateAssetCategoryRequest(
        Map<String, String> name,
        Integer compensationPercent,
        Map<String, String> description
) {}
