package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.CreateAssetCategoryRequest;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.UpdateAssetCategoryRequest;

import java.util.List;
import java.util.UUID;

public interface AssetCategoryService {
    AssetCategoryDto createAssetCategory(CreateAssetCategoryRequest request);
    List<AssetCategoryDto> getAllAssetCategories();
    AssetCategoryDto getById(UUID id);
    AssetCategoryDto updateAssetCategory(UUID id,UpdateAssetCategoryRequest request);
    Boolean deleteAssetCategory(UUID id);
}
