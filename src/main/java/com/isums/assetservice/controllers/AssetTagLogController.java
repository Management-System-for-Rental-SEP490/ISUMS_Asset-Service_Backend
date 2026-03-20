package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.entities.AssetTagLog;
import com.isums.assetservice.infrastructures.abstracts.AssetTagLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/assets/tag-logs")
@RequiredArgsConstructor
public class AssetTagLogController {
    private final AssetTagLogService assetTagLogService;

    @GetMapping("/tag/{tagValue}")
    public ApiResponse<List<AssetTagLog>> getLogsByTagValue(@PathVariable String tagValue){
        List<AssetTagLog> res = assetTagLogService.getLogsByTag(tagValue);
        return ApiResponses.ok(res,"Get logs by tag value successfully");
    }

    @GetMapping("/asset/{assetId}")
    public ApiResponse<List<AssetTagLog>> getLogsByAssetId(@PathVariable UUID assetId){
        List<AssetTagLog> res = assetTagLogService.getLogsByAsset(assetId);
        return ApiResponses.ok(res,"Get logs by asset id successfully");
    }
}
