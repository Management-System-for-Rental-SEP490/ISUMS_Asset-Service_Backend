package com.isums.assetservice.domains.dtos.AssetItemDTO;

import java.util.UUID;

public record CreateAssetItemRequest (
     UUID houseId,
     UUID categoryId,
     String displayName,
     String serialNumber,
     String nfcId,
     int conditionPercent
){
}
