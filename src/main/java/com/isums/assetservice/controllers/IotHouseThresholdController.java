package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.ThresholdRequest;
import com.isums.assetservice.domains.dtos.ThresholdResponse;
import com.isums.assetservice.services.IotThresholdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets/houses/{houseId}/iot/thresholds")
@RequiredArgsConstructor
public class IotHouseThresholdController {

    private final IotThresholdService thresholdService;
    private final MessageSource messageSource;

    private String msg(String code) {
        try {
            return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return code;
        }
    }

    @GetMapping()
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN', 'TENANT')")
    public ApiResponse<List<ThresholdResponse>> getHouseThresholds(@PathVariable UUID houseId) {
        var res = thresholdService.getAllByHouse(houseId);
        return ApiResponses.ok(res, msg("iot.threshold_get"));
    }

    @PutMapping("/{metric}")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN', 'TENANT')")
    public ApiResponse<ThresholdResponse> upsertHouse(@PathVariable UUID houseId, @PathVariable String metric, @Valid @RequestBody ThresholdRequest req) {
        var res = thresholdService.upsertHouseLevel(houseId, metric, req);
        return ApiResponses.ok(res, msg("iot.threshold_upsert"));
    }

    @DeleteMapping("/{metric}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteHouse(@PathVariable UUID houseId, @PathVariable String metric) {
        thresholdService.deleteHouseLevel(houseId, metric);
        return ApiResponses.noContent();
    }
}
