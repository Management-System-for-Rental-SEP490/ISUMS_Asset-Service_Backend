package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.AreaPowerStateResponse;
import com.isums.assetservice.domains.dtos.AreaPowerToggleRequest;
import com.isums.assetservice.infrastructures.abstracts.AreaPowerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/assets/houses/{houseId}/areas/{areaId}/iot")
@RequiredArgsConstructor
public class AreaPowerController {

    private final AreaPowerService areaPowerService;
    private final MessageSource messageSource;

    private String msg(String code) {
        try {
            return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return code;
        }
    }

    @PutMapping("/power")
    public ApiResponse<AreaPowerStateResponse> togglePower(
            @PathVariable UUID houseId,
            @PathVariable UUID areaId,
            @RequestBody @Valid AreaPowerToggleRequest req,
            @AuthenticationPrincipal Jwt jwt) {

        UUID requesterId = UUID.fromString(jwt.getSubject());
        AreaPowerStateResponse res = areaPowerService.toggleAreaPower(houseId, areaId, req.action(), requesterId);
        return ApiResponses.ok(res, msg("iot.power_updated"));
    }

    @GetMapping("/power")
    public ApiResponse<AreaPowerStateResponse> getPowerState(
            @PathVariable UUID houseId,
            @PathVariable UUID areaId) {

        AreaPowerStateResponse res = areaPowerService.getAreaPowerState(houseId, areaId);
        return ApiResponses.ok(res, msg("iot.power_retrieved"));
    }
}
