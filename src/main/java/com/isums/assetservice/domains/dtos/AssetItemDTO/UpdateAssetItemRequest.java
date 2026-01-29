package com.isums.assetservice.domains.dtos.AssetItemDTO;

import com.isums.assetservice.domains.enums.AssetStatus;

public record UpdateAssetItemRequest(
        String displayName,
        String serialNumber,
        String nfcId,
        Integer conditionPercent,
        AssetStatus status
) {}

