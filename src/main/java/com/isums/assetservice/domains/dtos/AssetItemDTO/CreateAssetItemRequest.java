package com.isums.assetservice.domains.dtos.AssetItemDTO;

import com.isums.assetservice.domains.enums.AssetStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code displayName} is a multilingual map, e.g. {@code {"vi":"Bàn","en":"Table"}}.
 * Supply at least one entry; "vi" is the default fallback.
 */
public record CreateAssetItemRequest(
        UUID houseId,
        UUID functionAreaId,
        UUID categoryId,
        Map<String, String> displayName,
        String serialNumber,
        int conditionPercent,
        AssetStatus status,
        List<String> assetImages
) {
}
