package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateHouseRequest;
import com.isums.assetservice.domains.entities.AssetEvent;
import com.isums.assetservice.domains.entities.AssetTag;
import com.isums.assetservice.domains.enums.AssetEventType;
import com.isums.assetservice.infrastructures.abstracts.AssetItemService;
import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.CreateAssetItemRequest;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateAssetItemRequest;
import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.enums.AssetStatus;
import com.isums.assetservice.infrastructures.abstracts.IoTDeviceService;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetCategoryRepository;
import com.isums.assetservice.infrastructures.repositories.AssetEventRepository;
import com.isums.assetservice.infrastructures.repositories.AssetItemRepository;
import com.isums.assetservice.infrastructures.repositories.AssetTagRepository;
import com.isums.houseservice.grpc.HouseServiceGrpc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
@Service
@RequiredArgsConstructor
public class AssetItemServiceImpl implements AssetItemService {
    private final AssetCategoryRepository assetCategoryRepository;
    private final AssetEventRepository assetEventRepository;
    private final AssetMapper assetMapper;
    private final AssetItemRepository assetItemRepository;
    private final AssetTagRepository assetTagRepository;

    @Override
    public AssetItemDto CreateAssetItem(CreateAssetItemRequest request) {
        try {
            AssetCategory assetCategory = assetCategoryRepository
                    .findById(request.categoryId())
                    .orElseThrow(() -> new RuntimeException("AssetCategory not found"));

            AssetItem assetItem = AssetItem.builder()
                    .houseId(request.houseId())
                    .functionAreaId(request.functionalId())
                    .category(assetCategory)
                    .displayName((request.displayName()))
                    .serialNumber((request.serialNumber()))
                    .conditionPercent((request.conditionPercent()))
                    .status(request.status())
                    .build();

            AssetItem created = assetItemRepository.save(assetItem);

            return assetMapper.mapAssetItem(created);

        } catch (Exception ex) {
            throw new RuntimeException("Error to create asset item: " + ex.getMessage());
        }
    }

    @Override
    public List<AssetItemDto> GetAllAssetItems() {
        try {
            List<AssetItem> items = assetItemRepository.findAll();

            List<UUID> assetIds = items.stream()
                    .map(AssetItem::getId)
                    .toList();

            List<AssetTag> tags = assetTagRepository.findByAssetItemIdInAndIsActiveTrue(assetIds);

            Map<UUID, List<AssetTag>> tagMap =
                    tags.stream()
                            .collect(Collectors.groupingBy(
                                    tag -> tag.getAssetItem().getId()
                            ));

            return items.stream()
                    .map(asset -> assetMapper.mapAssetItem(
                            asset,
                            tagMap.get(asset.getId())
                    ))
                    .toList();


        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());
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

            if (request.conditionPercent() != null)
                assetItem.setConditionPercent(request.conditionPercent());

            if (request.status() != null)
                assetItem.setStatus(request.status());

            AssetItem updated = assetItemRepository.save(assetItem);

            return assetMapper.mapAssetItem(updated);

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

            assetItemRepository.save(assetItem);
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
            List<UUID> assetIds = assetItems.stream()
                    .map(AssetItem::getId)
                    .toList();

            List<AssetTag> tags = assetTagRepository.findByAssetItemIdInAndIsActiveTrue(assetIds);

            Map<UUID, List<AssetTag>> tagMap =
                    tags.stream()
                            .collect(Collectors.groupingBy(
                                    tag -> tag.getAssetItem().getId()
                            ));
            return assetItems.stream()
                    .map(asset -> assetMapper.mapAssetItem(
                            asset,
                            tagMap.get(asset.getId())
                    ))
                    .toList();
        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset items: " + ex.getMessage());
        }
    }

    @Transactional
    @Override
    public AssetItemDto updateHouseForAsset(UUID assetId, UpdateHouseRequest request, UUID userId) {
        try{
            AssetItem item = assetItemRepository.findById(assetId)
                    .orElseThrow(()-> new RuntimeException("Id not found"));

            UUID oldHouseId = item.getHouseId();
            if (oldHouseId.equals(request.newHouseId())) {
                throw new RuntimeException("Asset already in this house");
            }
            item.setHouseId(request.newHouseId());

            assetItemRepository.save(item);

            AssetEvent event = AssetEvent.builder()
                    .assetItem(item)
                    .eventType(AssetEventType.TRANSFERRED)
                    .description("Transfer form " + oldHouseId + " to " + request.newHouseId())
                    .createdAt(Instant.now())
                    .createBy(userId)
                    .build();

            assetEventRepository.save(event);

            return assetMapper.mapAssetItem(item);
        } catch (Exception ex) {
            throw new RuntimeException("Error to update new house id for asset items: " + ex.getMessage());
        }
    }

    @Override
    public void updateCondition(UUID assetId, Integer conditionScore) {
        AssetItem asset = assetItemRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        asset.setConditionPercent(conditionScore);

        assetItemRepository.save(asset);
    }

}
