package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.AcknowledgeAlertRequest;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.TenantAlertDto;
import com.isums.assetservice.domains.dtos.TenantAlertsFeedDto;
import com.isums.assetservice.services.TenantAlertsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Tenant Alerts", description = "Aggregate IoT safety alerts received from tenant houses (gas, fire, smoke, temperature) — landlord & manager only")
@RestController
@RequestMapping("/api/assets/tenant-alerts")
@RequiredArgsConstructor
public class TenantAlertsController {

    private final TenantAlertsService tenantAlertsService;

    @Operation(
            summary = "Aggregate alert feed across all houses in scope",
            description = "Landlord sees all houses; manager sees only houses in regions they manage. " +
                    "Severity is auto-classified: gas/smoke/fire/high-temp → CRITICAL life-safety; " +
                    "moderate temperature/humidity → WARNING; rest → INFO. " +
                    "Sort order: life-safety pending first, then by severity weight desc, then by timestamp desc."
    )
    @GetMapping("/feed")
    @PreAuthorize("hasAnyRole('LANDLORD', 'MANAGER', 'ADMIN')")
    public ApiResponse<TenantAlertsFeedDto> getFeed(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "lifeSafety", required = false) Boolean lifeSafety,
            @RequestParam(value = "daysBack", required = false) Integer daysBack,
            @RequestParam(value = "limit", required = false) Integer limit) {
        TenantAlertsFeedDto dto = tenantAlertsService.getFeed(
                jwt.getSubject(), severity, status, lifeSafety, daysBack, limit);
        return ApiResponses.ok(dto, "Tenant alerts feed");
    }

    @Operation(
            summary = "Acknowledge / mark alert as resolved",
            description = "Records the acknowledging user (landlord or manager), timestamp and optional resolution note. " +
                    "Manager can only acknowledge alerts of houses inside their managed regions."
    )
    @PostMapping("/{houseId}/{alertId}/acknowledge")
    @PreAuthorize("hasAnyRole('LANDLORD', 'MANAGER', 'ADMIN')")
    public ApiResponse<TenantAlertDto> acknowledge(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID houseId,
            @PathVariable String alertId,
            @RequestBody(required = false) @Valid AcknowledgeAlertRequest req) {
        TenantAlertDto dto = tenantAlertsService.acknowledge(jwt.getSubject(), houseId, alertId, req);
        return ApiResponses.ok(dto, "Alert acknowledged");
    }
}
