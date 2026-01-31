package com.isums.assetservice.services;


<<<<<<< Updated upstream
import com.isums.assetservice.abstracts.AssetCategoryService;
=======
import com.isums.assetservice.infrastructures.abstracts.AssetCategoryService;
>>>>>>> Stashed changes
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.CreateAssetCategoryRequest;
import com.isums.assetservice.domains.entities.AssetCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetCategoryServiceImpl implements AssetCategoryService {
    private final AssetCategoryQuery assetCategoryQuery;

    @Override
    public ApiResponse<AssetCategory> createAssetCategory(CreateAssetCategoryRequest request) {
        try{
            AssetCategory assetCategory = AssetCategory.builder()
                    .name(request.name())
                    .compensationPercent(request.compensationPercent())
                    .description(request.description())
                    .build();

            AssetCategory created = assetCategoryQuery.createAssetCategory(assetCategory);
            return ApiResponses.created(created,"Create asset category successfully");
        } catch (Exception ex) {
            return ApiResponses.fail(HttpStatus.INTERNAL_SERVER_ERROR,"fail to create item" + ex.getMessage());
        }
    }

    @Override
    public ApiResponse<List<AssetCategoryDto>> getAllAssetCategories() {
        try{
            List<AssetCategoryDto> mapAssetCategories = assetCategoryQuery.getAllAsset();
            return ApiResponses.ok(mapAssetCategories,"Get all asset categories successfully");
        } catch (Exception ex) {
            return ApiResponses.fail(HttpStatus.INTERNAL_SERVER_ERROR,"fail to create item" + ex.getMessage());
        }
    }
}
