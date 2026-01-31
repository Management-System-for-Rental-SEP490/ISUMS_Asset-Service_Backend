package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.CreateAssetCategoryRequest;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.UpdateAssetCategoryRequest;
import com.isums.assetservice.domains.entities.AssetCategory;

import java.util.List;
import java.util.UUID;

public interface AssetCategoryService {
    ApiResponse<AssetCategory> createAssetCategory(CreateAssetCategoryRequest request);
    ApiResponse<List<AssetCategoryDto>> getAllAssetCategories();
    ApiResponse<AssetCategoryDto> updateAssetCategory(UUID id,UpdateAssetCategoryRequest request);
    ApiResponse<Void> deleteAssetCategory(UUID id);

}
