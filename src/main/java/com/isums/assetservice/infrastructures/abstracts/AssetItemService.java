package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetItemDTO.CreateAssetItemRequest;
import com.isums.assetservice.domains.entities.AssetItem;

import java.util.List;

public interface AssetItemService {
    ApiResponse<AssetItem> CreateAssetItem(CreateAssetItemRequest request);
    ApiResponse<List<AssetItem>> GetAllAssetItems();
}
