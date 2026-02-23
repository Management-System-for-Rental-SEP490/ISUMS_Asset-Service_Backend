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
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetCategoryRepository;
import com.isums.assetservice.infrastructures.repositories.AssetItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetItemServiceImpl implements AssetItemService {
    private final AssetCategoryRepository assetCategoryRepository;
    private final AssetMapper assetMapper;
    private final AssetItemRepository assetItemRepository;

    @Override
    public AssetItem CreateAssetItem(CreateAssetItemRequest request) {
        try {
            AssetCategory assetCategory = assetCategoryRepository
                    .findById(request.categoryId())
                    .orElseThrow(() -> new RuntimeException("AssetCategory not found"));

            AssetItem assetItem = AssetItem.builder()
                    .houseId(request.houseId())
                    .category(assetCategory)
                    .displayName((request.displayName()))
                    .serialNumber((request.serialNumber()))
                    .nfcId((request.nfcId()))
                    .conditionPercent((request.conditionPercent()))
                    .status(request.status())
                    .build();

            return assetItemRepository.save(assetItem);

        } catch (Exception ex) {
            throw new RuntimeException("Error to create asset item: " + ex.getMessage());
        }
    }

    @Override
    public List<AssetItemDto> GetAllAssetItems() {
        try {
            List<AssetItem> mapAssetItems = assetItemRepository.findAll();
            return assetMapper.mapAssetItems(mapAssetItems);

        } catch (Exception ex) {
            throw new RuntimeException("Error to create asset item: " + ex.getMessage());
        }
    }


    @Override
    public AssetItemDto UpdateAssetItem(UUID id, UpdateAssetItemRequest request) {
        try {
            AssetItem assetItem = assetItemRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Id not found"));


            if (request.conditionPercent() != null) {
                if (request.conditionPercent() < 0 || request.conditionPercent() > 100) {
                    throw new RuntimeException("condition must be in 0-100");                }
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

            AssetItem updated = assetItemRepository.save(assetItem);

            return assetMapper.mapAssetItem(assetItem);

        } catch (Exception ex) {
            throw new RuntimeException("Error to create asset item: " + ex.getMessage());
        }
    }

    @Override
    public Boolean deleteAssetItem(UUID id) {
        try {
            AssetItem assetItem = assetItemRepository.findById(id)
                    .orElseThrow(()-> new RuntimeException("Id not found"));


            assetItem.setStatus(AssetStatus.DISPOSED);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException("Error to create asset item: " + ex.getMessage());
        }
    }

    @Override
    public AssetItemDto getAssetItemById(UUID id) {
        try {

            AssetItem assetItem = assetItemRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("AssetItem with ID: " + id + " not found"));

            return assetMapper.mapAssetItem(assetItem);

        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());
        }
    }

    @Override
    public List<AssetItemDto> getAssetItemsByHouseId(UUID houseId) {
        try {
            List<AssetItem> assetItems = assetItemRepository.findByHouseId(houseId);
            return assetMapper.mapAssetItems(assetItems);
        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset items: " + ex.getMessage());
        }
    }
}
