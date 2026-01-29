package com.isums.assetservice.abstracts;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.CreateAssetCategoryRequest;
import com.isums.assetservice.domains.entities.AssetCategory;

import java.util.List;

public interface AssetCategoryService {
    ApiResponse<AssetCategory> createAssetCategory(CreateAssetCategoryRequest request);
    ApiResponse<List<AssetCategoryDto>> getAllAssetCategories();
}
