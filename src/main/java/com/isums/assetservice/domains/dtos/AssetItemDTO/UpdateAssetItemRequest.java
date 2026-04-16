package com.isums.assetservice.domains.dtos.AssetItemDTO;

import com.isums.assetservice.domains.enums.AssetStatus;

import java.util.Map;
import java.util.UUID;

/**
 * {@code displayName} is a multilingual map, e.g. {@code {"vi":"Bàn","en":"Table"}}.
 * Omit the field (null) to leave existing translations unchanged.
 * Supply a non-null map to fully replace all existing translations for this asset.
 */
public record UpdateAssetItemRequest(
        UUID functionAreaId,
        Map<String, String> displayName,
        String serialNumber,
        String nfcId,
        Integer conditionPercent,
        String note,
        AssetStatus status
) {}

