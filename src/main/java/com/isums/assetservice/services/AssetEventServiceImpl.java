package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AssetEventDTO.UpdateAssetEventRequest;
import com.isums.assetservice.domains.dtos.AssetImageDto;
import com.isums.assetservice.domains.entities.AssetImage;
import com.isums.assetservice.infrastructures.abstracts.AssetEventService;
import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.entities.AssetEvent;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetEventRepository;
import com.isums.assetservice.infrastructures.repositories.AssetImageRepository;
import com.isums.assetservice.infrastructures.repositories.AssetItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetEventServiceImpl implements AssetEventService {
    private final AssetEventRepository assetEventRepository;
    private final AssetImageRepository assetImageRepository;
    private final AssetMapper assetMapper;
    private final S3ServiceImpl s3;

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
        try{
            List<AssetEvent> assetEventDto = assetEventRepository.findAll();

            return assetMapper.maAssetEvents(assetEventDto);
        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());
        }
    }

    @Override
    public AssetEventDto updateEventStatus(UUID id, UpdateAssetEventRequest request) {
        try{
            AssetEvent event = assetEventRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Id not found"));
            if(request.status() == null){
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
        try {

            List<AssetEvent> events = assetEventRepository.findByJobIdWithAsset(jobId);

                return assetMapper.maAssetEvents(events);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot get events by job: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public AssetEventDto getLatestEvent(UUID assetId) {

        List<AssetEvent> list = assetEventRepository
                .findLatestEvent(assetId);

        if (list.isEmpty()) {
            return null;
        }

        AssetEvent e = list.getFirst();

        return new AssetEventDto(
                e.getId(),
                e.getEventType(),
                e.getPreviousCondition(),
                e.getCurrentCondition(),
                e.getNote(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getAssetItem().getId(),
                e.getAssetItem().getDisplayName(),
                getAssetImages(assetId)
        );
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
}
