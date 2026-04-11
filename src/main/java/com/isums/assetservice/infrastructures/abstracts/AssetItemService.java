package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.dtos.AssetImageDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.CreateAssetItemRequest;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateHouseRequest;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateAssetItemRequest;
import com.isums.assetservice.domains.dtos.BatchUpdateAssetRequest;
import com.isums.assetservice.domains.dtos.BatchUpdateResponse;
import com.isums.assetservice.domains.dtos.ConfirmAssetRequest;
import com.isums.assetservice.domains.enums.AssetStatus;
import common.paginations.dtos.PageRequest;
import common.paginations.dtos.PageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AssetItemService {
    AssetItemDto CreateAssetItem(CreateAssetItemRequest request);
    PageResponse<AssetItemDto> getAll(PageRequest request);
    AssetItemDto UpdateAssetItem(UUID id,UpdateAssetItemRequest request);
    Boolean deleteAssetItem(UUID id);
    AssetItemDto getAssetItemById(UUID id);
    List<AssetItemDto> getAssetItemsByHouseId(UUID houseId);
    AssetItemDto updateHouseForAsset(UUID assetId, UpdateHouseRequest request, UUID userId);
    void updateCondition(UUID assetId, Integer conditionScore);
    AssetItemDto  uploadAssetImages(UUID assetId, List<MultipartFile> files);
    void deleteAssetImage(UUID assetId, UUID imageId);
    BatchUpdateResponse batchUpdateAssetCondition(UUID staffId, BatchUpdateAssetRequest request);
    AssetItemDto confirmAsset(UUID assetId, AssetStatus newStatus);
}
