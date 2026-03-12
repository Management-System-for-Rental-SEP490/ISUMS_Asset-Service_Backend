package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.CreateAssetItemRequest;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateHouseRequest;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateAssetItemRequest;

import java.util.List;
import java.util.UUID;

public interface AssetItemService {
    AssetItemDto CreateAssetItem(CreateAssetItemRequest request);
    List<AssetItemDto> GetAllAssetItems();
    AssetItemDto UpdateAssetItem(UUID id,UpdateAssetItemRequest request);
    Boolean deleteAssetItem(UUID id);
    AssetItemDto getAssetItemById(UUID id);
    List<AssetItemDto> getAssetItemsByHouseId(UUID houseId);
    AssetItemDto updateHouseForAsset(UUID assetId, UpdateHouseRequest request, UUID userId);
    void updateCondition(UUID assetId, Integer conditionScore);
}
