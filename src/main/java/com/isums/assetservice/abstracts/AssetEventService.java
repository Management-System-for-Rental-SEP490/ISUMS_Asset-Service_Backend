package com.isums.assetservice.abstracts;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.dtos.AssetEventDTO.CreateAssetEventRequest;
import com.isums.assetservice.domains.dtos.AssetImageDTO.AssetImageDto;
import com.isums.assetservice.domains.dtos.AssetImageDTO.CreateAssetImageRequest;
import com.isums.assetservice.domains.entities.AssetEvent;

import java.util.List;

public interface AssetEventService {
    ApiResponse<AssetEventDto> createEvent(CreateAssetEventRequest request);
    ApiResponse<List<AssetEventDto>> getAllAssetEvents();
}
