package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AssetImageDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateHouseRequest;
import com.isums.assetservice.domains.dtos.BatchUpdateAssetRequest;
import com.isums.assetservice.domains.dtos.BatchUpdateResponse;
import com.isums.assetservice.domains.dtos.ConfirmAssetRequest;
import com.isums.assetservice.domains.entities.*;
import com.isums.assetservice.domains.enums.AssetEventType;
import com.isums.assetservice.exceptions.NotFoundException;
import com.isums.assetservice.infrastructures.abstracts.AssetItemService;
import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.CreateAssetItemRequest;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateAssetItemRequest;
import com.isums.assetservice.domains.enums.AssetStatus;
import com.isums.assetservice.infrastructures.abstracts.IoTDeviceService;
import com.isums.assetservice.infrastructures.grpcs.GrpcUserClient;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.*;
import com.isums.houseservice.grpc.HouseServiceGrpc;
import com.isums.userservice.grpc.UserResponse;
import common.statics.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final S3ServiceImpl s3;
    private final AssetImageRepository assetImageRepository;
    private final GrpcUserClient grpcUserClient;

    @Override
    public AssetItemDto CreateAssetItem(CreateAssetItemRequest request) {
        try {
            AssetCategory assetCategory = assetCategoryRepository
                    .findById(request.categoryId())
                    .orElseThrow(() -> new RuntimeException("AssetCategory not found"));

            AssetItem assetItem = AssetItem.builder()
                    .houseId(request.houseId())
                    .functionAreaId(request.functionAreaId())
                    .category(assetCategory)
                    .displayName((request.displayName()))
                    .serialNumber((request.serialNumber()))
                    .conditionPercent((request.conditionPercent()))
                    .status(AssetStatus.WAITING_MANAGER_CONFIRM)
                    .build();

            AssetItem created = assetItemRepository.save(assetItem);

            assetItem.getEvents().add(
                    AssetEvent.builder()
                            .eventType(AssetEventType.CREATED)
                            .note("Asset created, waiting for manager approval")
                            .createdAt(Instant.now())
                            .assetItem(assetItem)
                            .build()
            );

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

            if(request.functionAreaId() != null){
                assetItem.setFunctionAreaId((request.functionAreaId()));
            }

            if (request.displayName() != null)
                assetItem.setDisplayName(request.displayName());

            if (request.serialNumber() != null)
                assetItem.setSerialNumber(request.serialNumber());

            if (request.conditionPercent() != null)
                assetItem.setConditionPercent(request.conditionPercent());

            if(request.note() != null){
                assetItem.setNote(request.note());
            }

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
                    .note("Transfer form " + oldHouseId + " to " + request.newHouseId())
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

    @Override
    @Transactional
    public void uploadAssetImages(UUID assetId, List<MultipartFile> files) {
        boolean isExist = assetItemRepository.existsById(assetId);
        if(!isExist){
            throw new NotFoundException("Asset not found :  " + assetId);
        }

        AssetItem item = assetItemRepository.getReferenceById(assetId);

        files.forEach(file -> {
            String key = s3.upload(file,"asset/" + assetId);

            AssetImage image = AssetImage.builder()
                    .assetItem(item)
                    .key(key)
                    .build();

            assetImageRepository.save(image);
        });
    }

    @Override
    public List<AssetImageDto> getAssetImages(UUID assetId) {
        List<AssetImage> images = assetImageRepository.findByAssetItemId(assetId);

        List<AssetImageDto> imageDto = new ArrayList<>();
        images.forEach(image ->{
            String url = s3.getImageUrl(image.getKey());
            imageDto.add(new AssetImageDto(image.getId(),url,image.getCreatedAt()));
        });

        return imageDto;
    }

    @Override
    public void deleteAssetImage(UUID assetId, UUID imageId) {
        AssetImage image = assetImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("House image not found"));
        s3.delete(image.getKey());
        assetImageRepository.delete(image);
    }

    @Override
    @Transactional
    public BatchUpdateResponse batchUpdateAssetCondition(UUID staffId, BatchUpdateAssetRequest request) {
        try {

            List<UUID> ids = request.updates().stream()
                    .map(BatchUpdateAssetRequest.AssetUpdateItem::assetId)
                    .toList();

            List<AssetItem> assets = assetItemRepository.findAllById(ids);

            if (assets.size() != ids.size()) {
                throw new RuntimeException("Some assets not found");
            }

            Map<UUID, BatchUpdateAssetRequest.AssetUpdateItem> map = request.updates().stream()
                    .collect(Collectors.toMap(BatchUpdateAssetRequest.AssetUpdateItem::assetId, a -> a));

            UUID jobId = request.jobId();
            if (request.jobId() == null) {
                throw new RuntimeException("jobId is required");
            }
            for (AssetItem asset : assets) {

                BatchUpdateAssetRequest.AssetUpdateItem update = map.get(asset.getId());

                Integer oldCondition = asset.getConditionPercent();
                Integer newCondition = update.conditionPercent();
                AssetStatus oldStatus = asset.getStatus();
                AssetStatus newStatus = update.status();
                boolean hasChange = false;

                if (newCondition != null) {
                    if (newCondition < 0 || newCondition > 100) {
                        throw new RuntimeException("condition must be 0-100");
                    }

                    if (!newCondition.equals(oldCondition)) {
                        asset.setConditionPercent(newCondition);
                        hasChange = true;
                    }
                }

                if (update.note() != null && !update.note().equals(asset.getNote())) {
                    asset.setNote(update.note());
                    hasChange = true;
                }

                if (newStatus != null) {
                    if (newStatus != AssetStatus.BROKEN) {
                        throw new RuntimeException("Staff can only set status to BROKEN");
                    }

                    if (oldStatus != AssetStatus.BROKEN) {
                        asset.setStatus(AssetStatus.BROKEN);
                        hasChange = true;
                    }
                }

                if (hasChange) {
                    asset.getEvents().add(AssetEvent.builder()
                                    .eventType(AssetEventType.MAINTENANCE)
                                    .previousCondition(oldCondition)
                                    .currentCondition(asset.getConditionPercent())
                                    .note(asset.getNote())
                                    .createdAt(Instant.now())
                                    .createBy(staffId)
                                    .jobId(jobId)
                                    .assetItem(asset)
                                    .build()
                    );
                }
            }

            List<AssetItem> saved = assetItemRepository.saveAll(assets);

            return new BatchUpdateResponse(
                    request.updates().size(),
                    saved.size(),
                    assetMapper.mapAssetItems(saved)
            );

        } catch (Exception ex) {
            throw new RuntimeException("Error batch update asset: " + ex.getMessage());
        }
    }

    @Override
    public AssetItemDto confirmAsset(UUID assetId, AssetStatus newStatus) {
        try {

            Jwt jwt = (Jwt) SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getPrincipal();

            String keycloakId = jwt.getSubject();

            UserResponse userProfile = grpcUserClient
                    .getUserIdAndRoleByKeyCloakId(keycloakId);

            var roles = userProfile.getRolesList();

            if (!roles.contains(Roles.MANAGER) && !roles.contains(Roles.LANDLORD)) {
                throw new RuntimeException("Permission denied");
            }

            AssetItem asset = assetItemRepository.findById(assetId)
                    .orElseThrow(() -> new RuntimeException("Asset not found"));

            if (asset.getStatus() != AssetStatus.WAITING_MANAGER_CONFIRM) {
                throw new RuntimeException("Asset is not in waiting state");
            }

            if (newStatus == AssetStatus.IN_USE) {
                asset.setStatus(AssetStatus.IN_USE);

            } else if (newStatus == AssetStatus.DELETED) {
                asset.setStatus(AssetStatus.DELETED);

            } else {
                throw new RuntimeException("Invalid status");
            }

            asset.setUpdateAt(Instant.now());

            AssetItem saved = assetItemRepository.save(asset);

            return assetMapper.mapAssetItem(saved);

        } catch (Exception ex) {
            throw new RuntimeException("Error confirm asset: " + ex.getMessage());
        }

    }

}
