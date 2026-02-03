package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.dtos.AssetEventDTO.CreateAssetEventRequest;
import com.isums.assetservice.domains.dtos.AssetImageDTO.AssetImageDto;
import com.isums.assetservice.domains.dtos.AssetImageDTO.CreateAssetImageRequest;
import com.isums.assetservice.domains.entities.AssetEvent;

import java.util.List;

public interface AssetEventService {
    AssetEventDto createEvent(CreateAssetEventRequest request);
    List<AssetEventDto> getAllAssetEvents();
}
