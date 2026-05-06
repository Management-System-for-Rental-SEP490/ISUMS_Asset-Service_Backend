package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.dtos.AssetEventDTO.UpdateAssetEventRequest;
import com.isums.assetservice.domains.dtos.AssetEventImageDto;
import com.isums.assetservice.domains.entities.AssetEvent;
import com.isums.assetservice.domains.entities.AssetEventImage;
import com.isums.assetservice.domains.entities.AssetImage;
import com.isums.assetservice.domains.enums.AssetEventImageType;
import com.isums.assetservice.infrastructures.abstracts.AssetEventService;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetEventImageRepository;
import com.isums.assetservice.infrastructures.repositories.AssetEventRepository;
import com.isums.assetservice.infrastructures.repositories.AssetImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetEventServiceImpl implements AssetEventService {
    private final AssetEventRepository assetEventRepository;
    private final AssetEventImageRepository assetEventImageRepository;
    private final AssetMapper assetMapper;
    private final S3ServiceImpl s3;
    private final AssetImageRepository assetImageRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AssetEventDto> getAllAssetEvents() {
        try {
            List<AssetEvent> assetEvents = assetEventRepository.findAll();
            return assetMapper.maAssetEvents(assetEvents);
        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());
        }
    }

    @Override
    public AssetEventDto updateEventStatus(UUID id, UpdateAssetEventRequest request) {
        try {
            AssetEvent event = assetEventRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Id not found"));
            if (request.status() == null) {
                throw new RuntimeException("Status is required");
            }

            event.setEventType(request.status());
            AssetEvent updated = assetEventRepository.save(event);
            return assetMapper.mapAssetEvent(updated);
        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetEventDto> getEventsByJob(UUID jobId) {
        return assetEventRepository.findByJobIdWithAsset(jobId).stream()
                .map(event -> {
                    AssetEventDto dto = assetMapper.mapAssetEvent(event);
                    ImageGroups imageGroups = resolveImageGroups(event);
                    dto.setOldImages(toImageDtos(imageGroups.beforeImages()));
                    dto.setImages(toImageDtos(imageGroups.afterImages()));
                    return dto;
                })
                .toList();
    }

    private ImageGroups resolveImageGroups(AssetEvent event) {
        List<AssetEventImage> beforeImages = getEventImageEntitiesByType(event.getId(), AssetEventImageType.BEFORE);
        List<AssetEventImage> afterImages = getAfterImageEntities(event.getId());
        return new ImageGroups(beforeImages, afterImages);
    }

    private List<AssetEventImage> getAfterImageEntities(UUID eventId) {
        List<AssetEventImage> afterImages = getEventImageEntitiesByType(eventId, AssetEventImageType.AFTER);
        if (!afterImages.isEmpty()) {
            return afterImages;
        }

        return assetEventImageRepository.findByEventIdOrderByCreatedAtAsc(eventId).stream()
                .filter(img -> img.getType() == null)
                .toList();
    }

    private List<AssetEventImageDto> toImageDtos(List<AssetEventImage> images) {
        return images.stream().map(this::toImageDto).toList();
    }

    private List<AssetEventImage> getEventImageEntitiesByType(UUID eventId, AssetEventImageType type) {
        return assetEventImageRepository.findByEventIdAndTypeOrderByCreatedAtAsc(eventId, type);
    }

    private AssetEventImageDto toImageDto(AssetEventImage img) {
        return new AssetEventImageDto(img.getId(), s3.getImageUrl(img.getKey()), img.getCreatedAt());
    }

    private record ImageGroups(List<AssetEventImage> beforeImages, List<AssetEventImage> afterImages) {
    }

    @Override
    @Transactional
    public AssetEventDto getLatestEvent(UUID assetId) {
        List<AssetEvent> events = assetEventRepository.findLatestEvent(assetId);
        AssetEvent event = events.getFirst();
        ImageGroups imageGroups = resolveImageGroups(event);

        return new AssetEventDto(
                event.getId(),
                event.getJobId(),
                event.getEventType(),
                event.getPreviousCondition(),
                event.getCurrentCondition(),
                event.getNote(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                event.getAssetItem().getId(),
                event.getAssetItem().getDisplayName() != null ? event.getAssetItem().getDisplayName().resolve() : null,
                toImageDtos(imageGroups.beforeImages()),
                toImageDtos(imageGroups.afterImages())
        );
    }

    @Override
    @Transactional
    public void uploadEventImages(UUID eventId, List<MultipartFile> files) {
        AssetEvent event = assetEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        UUID assetId = event.getAssetItem().getId();
        List<AssetImage> currentAssetImages = assetImageRepository.findByAssetItemId(assetId);

        snapshotBeforeImages(event, currentAssetImages);

        Set<String> existingAfterKeys = assetEventImageRepository
                .findByEventIdAndTypeOrderByCreatedAtAsc(eventId, AssetEventImageType.AFTER)
                .stream()
                .map(AssetEventImage::getKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<AssetImage> currentImagesToDelete = currentAssetImages.stream()
                .filter(image -> !existingAfterKeys.contains(image.getKey()))
                .toList();
        if (!currentImagesToDelete.isEmpty()) {
            assetImageRepository.deleteAll(currentImagesToDelete);
        }

        for (MultipartFile file : files) {
            Instant now = Instant.now();
            String key = s3.upload(file, "asset/" + assetId);

            assetImageRepository.save(
                    AssetImage.builder()
                            .assetItem(event.getAssetItem())
                            .key(key)
                            .createdAt(now)
                            .build()
            );

            assetEventImageRepository.save(
                    AssetEventImage.builder()
                            .event(event)
                            .key(key)
                            .type(AssetEventImageType.AFTER)
                            .createdAt(now)
                            .build()
            );
        }
    }

    private void snapshotBeforeImages(AssetEvent event, List<AssetImage> currentAssetImages) {
        boolean alreadySnapshotted = !assetEventImageRepository
                .findByEventIdAndTypeOrderByCreatedAtAsc(event.getId(), AssetEventImageType.BEFORE)
                .isEmpty();
        if (alreadySnapshotted) {
            return;
        }

        List<AssetEventImage> snapshots = new ArrayList<>();
        for (AssetImage img : currentAssetImages) {
            snapshots.add(AssetEventImage.builder()
                    .event(event)
                    .key(img.getKey())
                    .type(AssetEventImageType.BEFORE)
                    .createdAt(img.getCreatedAt() != null ? img.getCreatedAt() : Instant.now())
                    .build());
        }
        if (!snapshots.isEmpty()) {
            assetEventImageRepository.saveAll(snapshots);
        }
    }
}
