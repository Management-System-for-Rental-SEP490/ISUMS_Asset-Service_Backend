package com.isums.assetservice.domains.dtos.AssetItemDTO;

import com.isums.assetservice.domains.enums.AssetStatus;

import java.util.UUID;

public record CreateAssetItemRequest (
     UUID houseId,
     UUID categoryId,
     String displayName,
     String serialNumber,
     int conditionPercent,
     AssetStatus status
){
}
