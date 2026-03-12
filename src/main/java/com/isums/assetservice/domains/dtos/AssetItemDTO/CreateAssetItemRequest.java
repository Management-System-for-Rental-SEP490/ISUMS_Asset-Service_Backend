package com.isums.assetservice.domains.dtos.AssetItemDTO;

import com.isums.assetservice.domains.enums.AssetStatus;

import java.util.UUID;

public record CreateAssetItemRequest (
     UUID houseId,
     UUID functionalId,
     UUID categoryId,
     String displayName,
     String serialNumber,
     String nfcId,
     int conditionPercent,
     AssetStatus status,
     Boolean isIoTDevice
){
}
