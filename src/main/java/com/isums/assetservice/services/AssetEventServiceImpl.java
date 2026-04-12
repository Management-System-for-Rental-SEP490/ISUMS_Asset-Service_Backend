package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AssetEventDTO.UpdateAssetEventRequest;
import com.isums.assetservice.domains.dtos.AssetEventImageDto;
import com.isums.assetservice.domains.entities.AssetEventImage;
import com.isums.assetservice.domains.entities.AssetImage;
import com.isums.assetservice.infrastructures.abstracts.AssetEventService;
import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.entities.AssetEvent;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetEventImageRepository;
import com.isums.assetservice.infrastructures.repositories.AssetEventRepository;
import com.isums.assetservice.infrastructures.repositories.AssetImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class AssetEventServiceImpl implements AssetEventService {
    private final AssetEventRepository assetEventRepository;
    private final AssetEventImageRepository assetEventImageRepository;
    private final AssetMapper assetMapper;
    private final S3ServiceImpl s3;
    private final AssetImageRepository assetImageRepository;

//    @Override
//    public AssetEventDto createEvent(CreateAssetEventRequest request) {
//        try{
//            AssetItem assetItem = assetItemRepository
//                    .findById(request.assetId())
//                    .orElseThrow(() -> new RuntimeException("ItemId not found"));
//
//            AssetEvent assetEvent = AssetEvent.builder()
//                    .assetItem(assetItem)
//                    .description(request.description())
//                    .eventType(AssetEventType.CREATED)
//                    .createdAt(Instant.now())
//                    .createBy(request.createBy())
//                    .build();
//
//            AssetEvent create = assetEventRepository.save(assetEvent);
//            return assetMapper.mapAssetEvent(create);
//
//        } catch (Exception ex) {
//            throw new RuntimeException("Error to get asset item: " + ex.getMessage());
//        }
//    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetEventDto> getAllAssetEvents() {
        try {
            List<AssetEvent> assetEventDto = assetEventRepository.findAll();

            return assetMapper.maAssetEvents(assetEventDto);
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
    public List<AssetEventDto> getEventsByJob(UUID jobId) {
        List<AssetEvent> events = assetEventRepository.findByJobIdWithAsset(jobId);

        return events.stream()
                .map(event -> {
                    AssetEventDto dto = assetMapper.mapAssetEvent(event);

                    dto.setImages(
                            assetEventImageRepository.findByEventId(event.getId())
                                    .stream()
                                    .map(img -> new AssetEventImageDto(
                                            img.getId(),
                                            s3.getImageUrl(img.getKey()),
                                            img.getCreatedAt()
                                    ))
                                    .toList()
                    );

                    List<AssetEvent> previousEvents = assetEventRepository
                            .findPreviousEvent(event.getAssetItem().getId());

                    if (!previousEvents.isEmpty()) {
                        dto.setOldImages(
                                assetEventImageRepository.findByEventId(previousEvents.getFirst().getId())
                                        .stream()
                                        .map(img -> new AssetEventImageDto(
                                                img.getId(),
                                                s3.getImageUrl(img.getKey()),
                                                img.getCreatedAt()
                                        ))
                                        .toList()
                        );
                    }

                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional
    public AssetEventDto getLatestEvent(UUID assetId) {

        List<AssetEvent> newList = assetEventRepository
                .findLatestEvent(assetId);

        AssetEvent e = newList.getFirst();

        return new AssetEventDto(
                e.getId(),
                e.getJobId(),
                e.getEventType(),
                e.getPreviousCondition(),
                e.getCurrentCondition(),
                e.getNote(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getAssetItem().getId(),
                e.getAssetItem().getDisplayName(),
                null,
                getEventImages(e.getId())
        );
    }

    @Override
    public void uploadEventImages(UUID eventId, List<MultipartFile> files) {
        AssetEvent event = assetEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        UUID assetId = event.getAssetItem().getId();

        // ❌ delete current images
        List<AssetImage> oldImages = assetImageRepository.findByAssetItemId(assetId);
        assetImageRepository.deleteAll(oldImages);

        for (MultipartFile file : files) {

            String key = s3.upload(file, "asset/" + assetId);

            // current
            assetImageRepository.save(
                    AssetImage.builder()
                            .assetItem(event.getAssetItem())
                            .key(key)
                            .createdAt(Instant.now())
                            .build()
            );

            // history
            assetEventImageRepository.save(
                    AssetEventImage.builder()
                            .event(event)
                            .key(key)
                            .createdAt(Instant.now())
                            .build()
            );
        }
    }

    private List<AssetEventImageDto> getEventImages(UUID eventId) {

        List<AssetEventImage> images =
                assetEventImageRepository.findByEventId(eventId);

        return images.stream()
                .map(img -> new AssetEventImageDto(
                        img.getId(),
                        s3.getImageUrl(img.getKey()),
                        img.getCreatedAt()
                ))
                .toList();
    }
}
