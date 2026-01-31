package com.isums.assetservice.services;

import com.isums.assetservice.infrastructures.abstracts.AssetEventService;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.dtos.AssetEventDTO.CreateAssetEventRequest;
import com.isums.assetservice.domains.entities.AssetEvent;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.enums.AssetEventType;
import com.isums.assetservice.domains.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetEventServiceImpl implements AssetEventService {
    private final AssetEventQuery assetEventQuery;
    private final AssetItemRepository assetItemRepository;
    private final AssetMapper assetMapper;

    @Override
    public ApiResponse<AssetEventDto> createEvent(CreateAssetEventRequest request) {
        try{
            AssetItem assetItem = assetItemRepository
                    .findById(request.assetId())
                    .orElseThrow(() -> new RuntimeException("ItemId not found"));

            AssetEvent assetEvent = AssetEvent.builder()
                    .assetItem(assetItem)
                    .description(request.description())
                    .eventType(AssetEventType.CREATED)
                    .createdAt(Instant.now())
                    .createBy(request.createBy())
                    .build();

            AssetEvent create = assetEventQuery.createAsset(assetEvent);
            AssetEventDto assetEventDto = assetMapper.mapAssetEvent(assetEvent);

            return ApiResponses.created(assetEventDto,"Create event succesfully");
        } catch (Exception ex) {
            return ApiResponses.fail(HttpStatus.INTERNAL_SERVER_ERROR,"fail to create item " + ex.getMessage());
        }
    }

    @Override
    public ApiResponse<List<AssetEventDto>> getAllAssetEvents() {
        try{
            List<AssetEventDto> assetEventDtos = assetEventQuery.getAllAsset();

            return ApiResponses.ok(assetEventDtos,"Get all events successfully");
        } catch (Exception ex) {
            return ApiResponses.fail(HttpStatus.INTERNAL_SERVER_ERROR,"fail to create item " + ex.getMessage());
        }
    }
}
