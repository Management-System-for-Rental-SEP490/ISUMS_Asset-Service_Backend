package com.isums.assetservice.services;

import com.isums.assetservice.infrastructures.abstracts.AssetItemService;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.CreateAssetItemRequest;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateAssetItemRequest;
import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.enums.AssetStatus;
import com.isums.assetservice.domains.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetItemServiceImpl implements AssetItemService {
    private final AssetItemQuery assetItemQuery;
    private final AssetCategoryRepository assetCategoryRepository;
    private final AssetMapper assetMapper;

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
    public ApiResponse<List<AssetItemDto>> GetAllAssetItems() {
         try{
             List<AssetItemDto> mapAssetItems = assetItemQuery.GetAllAssetItems();
             return ApiResponses.ok(mapAssetItems,"Get all items successfully");

         } catch (Exception ex) {
             return ApiResponses.fail(HttpStatus.INTERNAL_SERVER_ERROR,"fail to get all items: " +ex.getMessage());
         }
    }

    @Override
    public ApiResponse<AssetItemDto> UpdateAssetItem(UUID id,UpdateAssetItemRequest request) {
        try{
            AssetItem assetItem = assetItemQuery.findById(id);
            if(assetItem == null){
                return ApiResponses.fail(HttpStatus.NOT_FOUND,"Id not found");
            }

            if(request.conditionPercent() != null){
                if(request.conditionPercent() <0 || request.conditionPercent() > 100){
                    return  ApiResponses.fail(HttpStatus.BAD_REQUEST,"conditionPercent must be 0-100");
                }
            }

            if (request.displayName() != null)
                assetItem.setDisplayName(request.displayName());

            if (request.serialNumber() != null)
                assetItem.setSerialNumber(request.serialNumber());

            if (request.nfcId() != null)
                assetItem.setNfcId(request.nfcId());

            if (request.conditionPercent() != null)
                assetItem.setConditionPercent(request.conditionPercent());

            if (request.status() != null)
                assetItem.setStatus(request.status());

            AssetItem updated = assetItemQuery.createAssetItem(assetItem);
            AssetItemDto assetItemDto = assetMapper.mapAssetItem(updated);
            return ApiResponses.ok(assetItemDto, "Update asset successfully");

        } catch (Exception ex) {
            return ApiResponses.fail(HttpStatus.INTERNAL_SERVER_ERROR,"fail to get all items: " +ex.getMessage());
        }
    }

    @Override
    public ApiResponse<Void> deleteAssetItem(UUID id) {
        try{
            AssetItem assetItem = assetItemQuery.findById(id);
            if (assetItem == null) {
                return ApiResponses.fail(HttpStatus.NOT_FOUND, "Asset not found");
            }

            assetItem.setStatus(AssetStatus.DISPOSED);
            assetItemQuery.createAssetItem(assetItem);

            return ApiResponses.ok(null, "Asset deleted (soft)");
        } catch (Exception ex) {
            return ApiResponses.fail(HttpStatus.INTERNAL_SERVER_ERROR,"fail to get all items: " +ex.getMessage());
        }
    }
}
