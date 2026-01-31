package com.isums.assetservice.services;

import com.isums.assetservice.infrastructures.abstracts.AssetItemService;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.AssetItemDTO.CreateAssetItemRequest;
import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.enums.AssetStatus;
import com.isums.assetservice.infrastructures.repositories.AssetCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetItemServiceImpl implements AssetItemService {
    private final AssetItemQuery assetItemQuery;
    private final AssetCategoryRepository assetCategoryRepository;

    @Override
    public ApiResponse<AssetItem> CreateAssetItem(CreateAssetItemRequest request) {
        try{
            AssetCategory assetCategory = assetCategoryRepository
                    .findById(request.categoryId())
                    .orElseThrow(() -> new RuntimeException("AssetCategory not found"));

            AssetItem assetItem =AssetItem.builder()
                    .houseId(request.houseId())
                    .category(assetCategory)
                    .displayName((request.displayName()))
                    .serialNumber((request.serialNumber()))
                    .nfcId((request.nfcId()))
                    .conditionPercent((request.conditionPercent()))
                    .status(AssetStatus.AVAILABLE)
                    .build();

            AssetItem created = assetItemQuery.createAssetItem(assetItem);

            return ApiResponses.created(created,"Item created successfully");
        } catch (Exception ex) {
            return ApiResponses.fail(HttpStatus.INTERNAL_SERVER_ERROR,"fail to create item" + ex.getMessage());
        }
    }

    @Override
    public ApiResponse<List<AssetItem>> GetAllAssetItems() {
         try{
             List<AssetItem> assetItems = assetItemQuery.GetAllAssetItems();
             return ApiResponses.created(assetItems,"Get all items successfully");

         } catch (Exception ex) {
             return ApiResponses.fail(HttpStatus.INTERNAL_SERVER_ERROR,"fail to get all items: " +ex.getMessage());
         }
    }
}
