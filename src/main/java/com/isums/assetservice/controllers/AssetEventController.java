package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.infrastructures.abstracts.AssetEventService;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.dtos.AssetEventDTO.CreateAssetEventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/asset/events")
@RequiredArgsConstructor
public class AssetEventController {
    private final AssetEventService assetEventService;

    @PostMapping
    public ApiResponse<AssetEventDto> createAssetEvent(@RequestBody CreateAssetEventRequest request){
        AssetEventDto response = assetEventService.createEvent(request);
        return ApiResponses.created(response,"Create Event successfully");
    }

    @GetMapping
    public ApiResponse<List<AssetEventDto>> GetAllEvents(){
        List<AssetEventDto> response = assetEventService.getAllAssetEvents();
        return ApiResponses.ok(response,"Get events successfully");
    }
}
