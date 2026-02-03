package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.CreateAssetItemRequest;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateAssetItemRequest;
import com.isums.assetservice.domains.entities.AssetItem;

import java.util.List;
import java.util.UUID;

public interface AssetItemService {
    ApiResponse<AssetItem> CreateAssetItem(CreateAssetItemRequest request);
    ApiResponse<List<AssetItemDto>> GetAllAssetItems();
    ApiResponse<AssetItemDto> UpdateAssetItem(UUID id,UpdateAssetItemRequest request);
    ApiResponse<Void> deleteAssetItem(UUID id);
    AssetItemDto getAssetItemById(UUID id);
}
