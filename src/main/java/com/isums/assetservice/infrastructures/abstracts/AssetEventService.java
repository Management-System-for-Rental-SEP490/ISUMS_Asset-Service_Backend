package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.dtos.AssetEventDTO.CreateAssetEventRequest;
import com.isums.assetservice.domains.dtos.AssetEventDTO.UpdateAssetEventRequest;
import com.isums.assetservice.domains.dtos.AssetImageDTO.AssetImageDto;
import com.isums.assetservice.domains.dtos.AssetImageDTO.CreateAssetImageRequest;
import com.isums.assetservice.domains.entities.AssetEvent;

import java.util.List;
import java.util.UUID;

public interface AssetEventService {
    AssetEventDto createEvent(CreateAssetEventRequest request);
    List<AssetEventDto> getAllAssetEvents();
    AssetEventDto updateEventStatus(UUID id, UpdateAssetEventRequest request);

}
