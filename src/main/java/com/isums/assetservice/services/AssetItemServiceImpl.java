package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.*;
import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateHouseRequest;
import com.isums.assetservice.domains.dtos.AssetTagDto.AssetTagDto;
import com.isums.assetservice.domains.entities.*;
import com.isums.assetservice.domains.enums.AssetEventImageType;
import com.isums.assetservice.domains.enums.AssetEventType;
import com.isums.assetservice.exceptions.NotFoundException;
import com.isums.assetservice.infrastructures.abstracts.AssetItemService;
import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.CreateAssetItemRequest;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateAssetItemRequest;
import com.isums.assetservice.domains.enums.AssetStatus;
import common.i18n.TranslationMap;
import com.isums.assetservice.infrastructures.abstracts.IoTDeviceService;
import com.isums.assetservice.infrastructures.grpcs.GrpcUserClient;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.*;
import com.isums.houseservice.grpc.HouseServiceGrpc;
import com.isums.userservice.grpc.UserResponse;
import common.paginations.cache.CachedPageService;
import common.paginations.converters.SpringPageConverter;
import common.paginations.dtos.PageRequest;
import common.paginations.dtos.PageResponse;
import common.paginations.specifications.SpecificationBuilder;
import common.statics.Roles;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
@Service
@RequiredArgsConstructor
@Slf4j
public class AssetItemServiceImpl implements AssetItemService {
    private final AssetCategoryRepository assetCategoryRepository;
    private final AssetEventRepository assetEventRepository;
    private final AssetMapper assetMapper;
    private final AssetItemRepository assetItemRepository;
    private final AssetTagRepository assetTagRepository;
    private final S3ServiceImpl s3;
    private final AssetImageRepository assetImageRepository;
    private final GrpcUserClient grpcUserClient;
    private final CachedPageService cachedPageService;
    private final AssetEventImageRepository assetEventImageRepository;
    private final TranslationAutoFillService translationAutoFillService;

    private static final String PAGE_NS = "assets";
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
                    .displayName(translationAutoFillService.complete(request.displayName()))
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
    @Transactional(readOnly = true)
    public PageResponse<AssetItemDto> getAll(PageRequest request) {
        return cachedPageService.getOrLoad(PAGE_NS, request, new TypeReference<>() {
                },
                () -> loadPage(request)
        );
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
                assetItem.setDisplayName(translationAutoFillService.complete(request.displayName()));

            if (request.serialNumber() != null)
                assetItem.setSerialNumber(request.serialNumber());

            if (request.conditionPercent() != null)
                assetItem.setConditionPercent(request.conditionPercent());

            if(request.note() != null){
                assetItem.setNote(request.note());
                assetItem.setNoteTranslations(translationAutoFillService.complete(Map.of("vi", request.note())));
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

            AssetItemDto dto = assetMapper.mapAssetItem(assetItem);

            List<AssetTag> tags = assetTagRepository
                    .findByAssetItemIdAndIsActiveTrue(id);

            dto.setTags(assetMapper.tagDtos(tags));

            dto.setImages(getAssetImages(id));

            return dto;

        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());
        }
    }

    @Override
    public List<AssetItemDto> getAssetItemsByHouseId(UUID houseId) {
        try {
            List<AssetItem> assetItems = assetItemRepository.findByHouseId(houseId);
            return hydrateAssetItems(assetItems);

        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset items: " + ex.getMessage());
        }
    }

    @Override
    public List<AssetItemDto> getAssetItemsByHouseIdAndFunctionAreaId(UUID houseId, UUID functionAreaId) {
        try {
            List<AssetItem> assetItems = assetItemRepository.findByHouseIdAndFunctionAreaId(houseId, functionAreaId);
            return hydrateAssetItems(assetItems);
        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset items by function area: " + ex.getMessage());
        }
    }

    @Override
    public List<AssetCountByFunctionAreaDto> getAssetCountByFunctionArea(UUID houseId) {
        try {
            return assetItemRepository.countByHouseIdGroupByFunctionAreaId(houseId);
        } catch (Exception ex) {
            throw new RuntimeException("Error to count asset items by function area: " + ex.getMessage());
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

        AssetEvent event = assetEventRepository.save(
                AssetEvent.builder()
                        .assetItem(item)
                        .createdAt(Instant.now())
                        .build()
        );

        List<AssetImage> currentAssetImages = assetImageRepository.findByAssetItemId(assetId);
        for (AssetImage prev : currentAssetImages) {
            assetEventImageRepository.save(
                    AssetEventImage.builder()
                            .event(event)
                            .key(prev.getKey())
                            .type(com.isums.assetservice.domains.enums.AssetEventImageType.BEFORE)
                            .createdAt(prev.getCreatedAt() != null ? prev.getCreatedAt() : Instant.now())
                            .build()
            );
        }

        assetImageRepository.deleteAll(currentAssetImages);

        List<AssetImageDto> imageDtos = new ArrayList<>();

        for (MultipartFile file : files) {

            String key = s3.upload(file, "asset/" + assetId);

            AssetImage saved = assetImageRepository.save(
                    AssetImage.builder()
                            .assetItem(item)
                            .key(key)
                            .createdAt(Instant.now())
                            .build()
            );

            assetEventImageRepository.save(
                    AssetEventImage.builder()
                            .event(event)
                            .key(key)
                            .type(com.isums.assetservice.domains.enums.AssetEventImageType.AFTER)
                            .createdAt(Instant.now())
                            .build()
            );

            imageDtos.add(new AssetImageDto(
                    saved.getId(),
                    s3.getImageUrl(key),
                    saved.getCreatedAt()
            ));

        }

        AssetItemDto dto = assetMapper.mapAssetItem(item);
        dto.setImages(imageDtos);
    }

    private List<AssetImageDto> getAssetImages(UUID assetId) {
        List<AssetImage> images = assetImageRepository.findByAssetItemId(assetId);

        List<AssetImageDto> imageDto = new ArrayList<>();
        images.forEach(image ->{
            String url = s3.getImageUrl(image.getKey());
            imageDto.add(new AssetImageDto(image.getId(),url,image.getCreatedAt()));
        });

        return imageDto;
    }

    private List<AssetItemDto> hydrateAssetItems(List<AssetItem> assetItems) {
        List<UUID> assetIds = assetItems.stream()
                .map(AssetItem::getId)
                .toList();

        List<AssetTag> tags = assetIds.isEmpty()
                ? List.of()
                : assetTagRepository.findByAssetItemIdInAndIsActiveTrue(assetIds);

        Map<UUID, List<AssetTag>> tagMap = tags.stream()
                .collect(Collectors.groupingBy(tag -> tag.getAssetItem().getId()));

        return assetItems.stream()
                .map(asset -> {
                    AssetItemDto dto = assetMapper.mapAssetItem(asset);
                    dto.setTags(assetMapper.tagDtos(tagMap.getOrDefault(asset.getId(), List.of())));
                    dto.setImages(getAssetImages(asset.getId()));
                    return dto;
                })
                .toList();
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
    public BatchUpdateResponse batchUpdateAssetCondition(
            UUID staffId,
            BatchUpdateAssetRequest request
    ) {
        try {
            List<UUID> ids = request.updates().stream()
                    .map(BatchUpdateAssetRequest.AssetUpdateItem::assetId)
                    .toList();

            List<AssetItem> assets = assetItemRepository.findAllById(ids);

            if (assets.size() != ids.size()) {
                throw new RuntimeException("Some assets not found");
            }

            Map<UUID, BatchUpdateAssetRequest.AssetUpdateItem> map =
                    request.updates().stream()
                            .collect(Collectors.toMap(
                                    BatchUpdateAssetRequest.AssetUpdateItem::assetId,
                                    a -> a
                            ));

            UUID jobId = request.jobId();
            if (jobId == null) {
                throw new RuntimeException("jobId is required");
            }

            List<BatchUpdateResponse.EventResult> results = new ArrayList<>();

            for (AssetItem asset : assets) {

                var update = map.get(asset.getId());

                Integer oldCondition = asset.getConditionPercent();
                Integer newCondition = update.conditionPercent();
                AssetStatus oldStatus = asset.getStatus();
                AssetStatus newStatus = update.status();

                boolean hasChange = false;

                if (newCondition != null && !newCondition.equals(oldCondition)) {
                    asset.setConditionPercent(newCondition);
                    hasChange = true;
                }

                if (update.note() != null && !update.note().equals(asset.getNote())) {
                    asset.setNote(update.note());
                    asset.setNoteTranslations(translationAutoFillService.complete(Map.of("vi", update.note())));
                    hasChange = true;
                }

                if (newStatus != null) {
                    if (newStatus != AssetStatus.BROKEN && newStatus != oldStatus) {
                        throw new RuntimeException("Staff can only set BROKEN or keep current");
                    }

                    if (newStatus == AssetStatus.BROKEN && oldStatus != AssetStatus.BROKEN) {
                        asset.setStatus(AssetStatus.BROKEN);
                        hasChange = true;
                    }
                }

                if (hasChange) {
                    AssetEvent event = AssetEvent.builder()
                            .eventType(AssetEventType.MAINTENANCE)
                            .previousCondition(oldCondition)
                            .currentCondition(asset.getConditionPercent())
                            .note(asset.getNote())
                            .createdAt(Instant.now())
                            .createBy(staffId)
                            .jobId(jobId)
                            .assetItem(asset)
                            .build();

                    AssetEvent savedEvent = assetEventRepository.save(event);
                    snapshotCurrentImagesForMaintenance(savedEvent);

                    results.add(new BatchUpdateResponse.EventResult(
                            asset.getId(),
                            event.getId()
                    ));
                }
            }

            assetItemRepository.saveAll(assets);

            return new BatchUpdateResponse(
                    request.updates().size(),
                    assets.size(),
                    results
            );

        } catch (Exception ex) {
            throw new RuntimeException("Error batch update asset: " + ex.getMessage());
        }
    }

    private void snapshotCurrentImagesForMaintenance(AssetEvent maintenanceEvent) {
        if (maintenanceEvent.getAssetItem() == null) {
            return;
        }

        UUID assetId = maintenanceEvent.getAssetItem().getId();
        List<AssetImage> currentImages = assetImageRepository.findByAssetItemId(assetId);

        if (currentImages.isEmpty()) {
            return;
        }

        List<AssetEventImage> snapshots = currentImages.stream()
                .filter(image -> image.getKey() != null)
                .map(image -> AssetEventImage.builder()
                        .event(maintenanceEvent)
                        .key(image.getKey())
                        .type(AssetEventImageType.BEFORE)
                        .createdAt(image.getCreatedAt() != null ? image.getCreatedAt() : Instant.now())
                        .build())
                .toList();

        if (!snapshots.isEmpty()) {
            assetEventImageRepository.saveAll(snapshots);
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

    private PageResponse<AssetItemDto> loadPage(PageRequest request) {
        AssetStatus statusFilter = request.<String>filterValue("status")
                .map(s -> {
                    try {
                        return AssetStatus.valueOf(s.toUpperCase().trim());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .orElse(null);

        String statusesRaw = request.<String>filterValue("statuses").orElse(null);

        String houseIdRaw = request.<String>filterValue("houseId").orElse(null);
        UUID houseIdFilter = houseIdRaw != null ? UUID.fromString(houseIdRaw) : null;

        var spec = SpecificationBuilder.<AssetItem>create()
                .keywordLike(request.keyword(), "name", "address")
                .enumEq("status", statusFilter)
                .enumInRaw("status", statusesRaw, AssetStatus.class)
                .eq("houseId", houseIdFilter)
                .build();

        var pageable = SpringPageConverter.toPageable(request);

        Page<AssetItem> page = assetItemRepository.findAll(spec, pageable);

        List<AssetItem> items = page.getContent();

        List<UUID> assetIds = items.stream()
                .map(AssetItem::getId)
                .toList();

        List<AssetTag> tags = assetTagRepository.findByAssetItemIdInAndIsActiveTrue(assetIds);

        Map<UUID, List<AssetTag>> tagMap = tags.stream().collect(Collectors.groupingBy(tag -> tag.getAssetItem().getId()));

        List<AssetItemDto> dtos = items.stream()
                .map(asset -> {

                    AssetItemDto dto = assetMapper.mapAssetItem(asset);

                    dto.setTags(
                            assetMapper.tagDtos(tagMap.getOrDefault(asset.getId(), List.of()))
                    );

                    dto.setImages(getAssetImages(asset.getId()));

                    return dto;
                })
                .toList();

        return PageResponse.of(
                dtos,
                page.hasNext(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    private Duration contractEndBuffer() {
        return Duration.ofDays(1);
    }

}

