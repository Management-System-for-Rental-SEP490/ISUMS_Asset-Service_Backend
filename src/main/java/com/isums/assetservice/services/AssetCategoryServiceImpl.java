package com.isums.assetservice.services;

import com.isums.assetservice.infrastructures.abstracts.AssetCategoryService;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.CreateAssetCategoryRequest;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.UpdateAssetCategoryRequest;
import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.domains.mapper.AssetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetCategoryServiceImpl implements AssetCategoryService {
    private final AssetCategoryQuery assetCategoryQuery;
    private final AssetMapper assetMapper;

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
            return ApiResponses.fail(HttpStatus.INTERNAL_SERVER_ERROR,"fail to get all categories" + ex.getMessage());
        }
    }

    @Override
    public ApiResponse<AssetCategoryDto> updateAssetCategory(UUID id,UpdateAssetCategoryRequest request) {
        try {
            AssetCategory assetCategory = assetCategoryQuery.findById(id);
            if(assetCategory == null){
                return ApiResponses.fail(HttpStatus.NOT_FOUND,"Id not found");
            }


            //Validation name
            if(request.name() != null && request.name().isBlank()){
                return ApiResponses.fail(HttpStatus.BAD_REQUEST,"Name can't be empty");
            }

            //Validation CompensationPercent
            if(request.compensationPercent() != null){
                if(request.compensationPercent() < 0 || request.compensationPercent() > 100){
                    return ApiResponses.fail(HttpStatus.BAD_REQUEST,"CompensationPercent must be 0 - 100");
                }
            }

            if(request.name() != null){
                assetCategory.setName(request.name());

            }
            if(request.compensationPercent() != null){
                assetCategory.setCompensationPercent(request.compensationPercent());

            }
            if(request.description() != null){
                assetCategory.setDescription(request.description());

            }

            AssetCategory updated = assetCategoryQuery.createAssetCategory(assetCategory);
            AssetCategoryDto assetCategoryDto = assetMapper.mapAssetCategory(updated);

            return ApiResponses.ok(assetCategoryDto,"Update asset category successfully");


        } catch (Exception ex) {
            return ApiResponses.fail(HttpStatus.INTERNAL_SERVER_ERROR,"fail to get categories" + ex.getMessage());
        }
    }

    @Override
    public ApiResponse<Void> deleteAssetCategory(UUID id) {
        return null;
    }
}
