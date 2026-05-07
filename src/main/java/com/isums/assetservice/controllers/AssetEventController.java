package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.AssetEventDTO.UpdateAssetEventRequest;
import com.isums.assetservice.infrastructures.abstracts.AssetEventService;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.dtos.AssetEventDTO.CreateAssetEventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets/events")
@RequiredArgsConstructor
public class AssetEventController {
    private final AssetEventService assetEventService;
    private final MessageSource messageSource;

    private String msg(String code) {
        try {
            return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return code;
        }
    }

    @GetMapping
    public ApiResponse<List<AssetEventDto>> GetAllEvents(){
        List<AssetEventDto> response = assetEventService.getAllAssetEvents();
        return ApiResponses.ok(response, msg("event.get_all"));
    }

    @PutMapping("/{id}")
    public ApiResponse<AssetEventDto> UpdateAssetEvent(@PathVariable UUID id, @RequestBody UpdateAssetEventRequest request){
        AssetEventDto response = assetEventService.updateEventStatus(id, request);
        return ApiResponses.ok(response, msg("event.update_status"));
    }

    @GetMapping("/job/{jobId}")
    public ApiResponse<List<AssetEventDto>> getEventsByJob(@PathVariable UUID jobId) {
        List<AssetEventDto> res = assetEventService.getEventsByJob(jobId);
        return ApiResponses.ok(res, msg("event.get_by_job"));
    }

    @GetMapping("/assets/{id}/latest-event")
    public ApiResponse<AssetEventDto> getLatest(@PathVariable UUID id){
        return ApiResponses.ok(assetEventService.getLatestEvent(id), msg("event.get_latest"));
    }

    @PostMapping(value = "/{eventId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> uploadEventImages(
            @PathVariable UUID eventId,
            @RequestParam("files") List<MultipartFile> files
    ) {
        assetEventService.uploadEventImages(eventId, files);
        return ApiResponses.ok(null, msg("event.upload_images"));
    }
}
