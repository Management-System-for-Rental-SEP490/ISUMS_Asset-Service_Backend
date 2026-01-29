package com.isums.assetservice.domains.dtos.AssetItemDTO;

import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.domains.enums.AssetStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetItemDto {
    private UUID id;
    private UUID houseId;
    private AssetCategory category;
    private String displayName;
    private String serialNumber;
    private String nfcId;
    private int conditionPercent;
    private AssetStatus status;
}
