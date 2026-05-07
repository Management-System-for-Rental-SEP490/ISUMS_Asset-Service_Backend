package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.CreateIotSafetyConfigRequest;
import com.isums.assetservice.domains.dtos.IotSafetyConfigDto;
import com.isums.assetservice.domains.dtos.IotSafetyConfigVersionDto;
import com.isums.assetservice.services.IotSafetyConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "IoT Safety Config", description = "Versioned indoor-safety scoring config (DB-backed, immutable history)")
@RestController
@RequestMapping("/api/assets/iot/safety-config")
@RequiredArgsConstructor
public class IotSafetyConfigController {

    private final IotSafetyConfigService service;

    @Operation(
            summary = "Get current active safety config",
            description = "Returns sensor profile, thresholds, score formula, capability gaps with citations. " +
                    "Mobile renders this without hardcoding any number; cached server-side via @Cacheable."
    )
    @GetMapping
    public ApiResponse<IotSafetyConfigDto> getCurrent() {
        return ApiResponses.ok(service.getCurrent(), "Success");
    }

    @Operation(
            summary = "Safety config version history (admin)",
            description = "All versions newest first; row with expiredAt=null is active."
    )
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('LANDLORD','MANAGER')")
    public ApiResponse<List<IotSafetyConfigVersionDto>> history() {
        return ApiResponses.ok(service.getHistory(), "Success");
    }

    @Operation(
            summary = "Publish new safety config version (LANDLORD)",
            description = "Creates immutable new version; auto-expires previously active row. " +
                    "Validates: scoreComponents weights must sum ≈ 1.0, thresholds[] and bands[] required."
    )
    @PostMapping
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<IotSafetyConfigVersionDto> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CreateIotSafetyConfigRequest request) {
        return ApiResponses.created(
                service.createVersion(actorId(jwt), request),
                "Safety config version published");
    }

    @Operation(summary = "Manually expire a version (LANDLORD)")
    @PatchMapping("/{id}/expire")
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<IotSafetyConfigVersionDto> expire(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        return ApiResponses.ok(
                service.expireVersion(id, actorId(jwt)),
                "Safety config version expired");
    }

    private UUID actorId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid JWT subject");
        }
    }
}
