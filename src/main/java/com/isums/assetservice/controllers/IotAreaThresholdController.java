package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.ThresholdRequest;
import com.isums.assetservice.domains.dtos.ThresholdResponse;
import com.isums.assetservice.services.IotThresholdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets/houses/{houseId}/areas/{areaId}/iot/thresholds")
@RequiredArgsConstructor
public class IotAreaThresholdController {
    private final IotThresholdService thresholdService;

    @GetMapping
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN', 'TENANT')")
    public ApiResponse<List<ThresholdResponse>> getAll(@PathVariable UUID houseId, @PathVariable UUID areaId) {
        var res = thresholdService.getAllByArea(areaId);
        return ApiResponses.ok(res, "Success to get thresholds");
    }

    @PutMapping("/{metric}")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN', 'TENANT')")
    public ApiResponse<ThresholdResponse> upsert(@PathVariable UUID houseId, @PathVariable UUID areaId, @PathVariable String metric, @Valid @RequestBody ThresholdRequest req) {
        var res = thresholdService.upsertAreaLevel(houseId, areaId, metric, req);
        return ApiResponses.ok(res, "Success to upsert threshold");
    }

    @DeleteMapping("/{metric}")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN', 'TENANT')")
    public ApiResponse<Void> delete(@PathVariable UUID houseId, @PathVariable UUID areaId, @PathVariable String metric) {
        thresholdService.deleteAreaLevel(areaId, metric);
        return ApiResponses.noContent();
    }
}
