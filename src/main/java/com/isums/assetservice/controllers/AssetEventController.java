package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.AssetEventDTO.UpdateAssetEventRequest;
import com.isums.assetservice.infrastructures.abstracts.AssetEventService;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.dtos.AssetEventDTO.CreateAssetEventRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets/events")
@RequiredArgsConstructor
public class AssetEventController {
    private final AssetEventService assetEventService;

//    @PostMapping
//    public ApiResponse<AssetEventDto> createAssetEvent(@RequestBody CreateAssetEventRequest request){
//        AssetEventDto response = assetEventService.createEvent(request);
//        return ApiResponses.created(response,"Create Event successfully");
//    }

    @GetMapping
    public ApiResponse<List<AssetEventDto>> GetAllEvents(){
        List<AssetEventDto> response = assetEventService.getAllAssetEvents();
        return ApiResponses.ok(response,"Get events successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<AssetEventDto> UpdateAssetEvent(@PathVariable UUID id, @RequestBody UpdateAssetEventRequest request){
        AssetEventDto response = assetEventService.updateEventStatus(id,request);
        return ApiResponses.ok(response,"Update status successfully");
    }

    @GetMapping("/job/{jobId}")
    public ApiResponse<List<AssetEventDto>> getEventsByJob(@PathVariable UUID jobId) {
        List<AssetEventDto> res = assetEventService.getEventsByJob(jobId);

        return ApiResponses.ok(res, "Get asset events by job successfully");
    }

    @GetMapping("/assets/{id}/latest-event")
    public ApiResponse<AssetEventDto> getLatest(@PathVariable UUID id){
        return ApiResponses.ok(assetEventService.getLatestEvent(id), "Success");
    }
}
