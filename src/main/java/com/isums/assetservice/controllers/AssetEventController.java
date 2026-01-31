package com.isums.assetservice.controllers;

import com.isums.assetservice.infrastructures.abstracts.AssetEventService;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.dtos.AssetEventDTO.CreateAssetEventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/asset-events")
@RequiredArgsConstructor
public class AssetEventController {
    private final AssetEventService assetEventService;

    public ResponseEntity<ApiResponse<AssetEventDto>> createAssetEvent(@RequestBody CreateAssetEventRequest request){
        ApiResponse<AssetEventDto> response = assetEventService.createEvent(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    public ResponseEntity<ApiResponse<List<AssetEventDto>>> GetAllEvents(){
        ApiResponse<List<AssetEventDto>> response = assetEventService.getAllAssetEvents();
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
