package com.isums.assetservice.domains.dtos.AssetItemDTO;

import com.isums.assetservice.domains.enums.AssetStatus;

import java.util.UUID;

public record UpdateAssetItemRequest(
        UUID functionAreaId,
        String displayName,
        String serialNumber,
        String nfcId,
        Integer conditionPercent,
        AssetStatus status
) {}

