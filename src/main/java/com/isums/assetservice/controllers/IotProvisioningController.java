package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.IotProvisionResponse;
import com.isums.assetservice.domains.dtos.*;
import com.isums.assetservice.domains.enums.Severity;
import com.isums.assetservice.infrastructures.abstracts.IotProvisioningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets/houses/{houseId}/iot")
@RequiredArgsConstructor
public class IotProvisioningController {

    private final IotProvisioningService iotProvisioningService;

    @PostMapping("/provision")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('MANAGER')")
    public ApiResponse<IotProvisionResponse> provision(@PathVariable UUID houseId, @RequestBody IotProvisionRequest req) {
        var res = iotProvisioningService.provisionController(houseId, req.areaId(), req.deviceId());
        return ApiResponses.created(res, "IoT controller provisioned successfully");
    }

    @DeleteMapping("/deprovision")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('MANAGER')")
    public ApiResponse<Void> deprovision(@PathVariable UUID houseId) {
        iotProvisioningService.deprovisionController(houseId);
        return ApiResponses.ok(null, "IoT controller deprovisioned successfully");
    }

    @GetMapping("/controller")
    public ApiResponse<ControllerInfoResponse> getController(@PathVariable UUID houseId) {
        var res = iotProvisioningService.getControllerByHouse(houseId);
        return ApiResponses.ok(res, "IoT controller retrieved successfully");
    }

    @PutMapping("/nodes/{thing}/assign-area")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ApiResponse<Void> assignArea(@PathVariable UUID houseId, @PathVariable String thing, @RequestBody AssignAreaRequest req) {
        iotProvisioningService.assignNodeToArea(thing, req.areaId());
        return ApiResponses.noContent();
    }

    @PostMapping("/provision-node")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ApiResponse<NodeProvisionResponse> provisionNode(@PathVariable UUID houseId, @RequestBody ProvisionNodeRequest req) {
        var res = iotProvisioningService.provisionNode(
                houseId, req.serial(), req.token(), req.areaId(),
                req.capabilities() != null ? req.capabilities() : Set.of());
        return ApiResponses.ok(res, "Node provisioned");
    }

    @PutMapping("/nodes/{thing}/capabilities")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ApiResponse<Void> updateCapabilities(@PathVariable UUID houseId, @PathVariable String thing, @RequestBody @Valid UpdateCapabilitiesRequest req) {
        iotProvisioningService.updateNodeCapabilities(thing, req.capabilities());
        return ApiResponses.noContent();
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN','TENANT')")
    public ApiResponse<PagedResponse<AlertDto>> getAlerts(@PathVariable UUID houseId, @RequestParam(defaultValue = "10") int limit, @RequestParam(required = false) String cursor,
                                                          @RequestParam(required = false) String date, @RequestParam(required = false) Severity level) {
        var res = iotProvisioningService.getAlerts(houseId, limit, cursor, date, level);
        return ApiResponses.ok(res, "Alerts retrieved");
    }

    @PostMapping("/cmd")
    @PreAuthorize("hasAnyRole('LANDLORD','MANAGER','TENANT')")
    public ApiResponse<Void> sendCommand(
            @PathVariable UUID houseId,
            @RequestBody IotCommandRequest req) {
        iotProvisioningService.sendCommand(houseId, req);
        return ApiResponses.ok(null, "Command sent");
    }

    @GetMapping("/ota/upload-url")
    @PreAuthorize("hasAnyRole('LANDLORD','MANAGER')")
    public ApiResponse<OtaUploadUrlResponse> getOtaUploadUrl(
            @PathVariable UUID houseId,
            @RequestParam String filename) {
        var res = iotProvisioningService.getOtaUploadUrl(houseId, filename);
        return ApiResponses.ok(res, "Upload URL generated");
    }

    @PostMapping("/ota")
    @PreAuthorize("hasAnyRole('LANDLORD','MANAGER')")
    public ApiResponse<OtaJobResponse> triggerOta(
            @PathVariable UUID houseId,
            @RequestBody OtaRequest req) {
        var res = iotProvisioningService.triggerOta(houseId, req);
        return ApiResponses.ok(res, "OTA triggered");
    }

    @GetMapping("/ota/{jobId}")
    @PreAuthorize("hasAnyRole('LANDLORD','MANAGER')")
    public ApiResponse<OtaStatusResponse> getOtaStatus(
            @PathVariable UUID houseId,
            @PathVariable String jobId) {
        var res = iotProvisioningService.getOtaStatus(houseId, jobId);
        return ApiResponses.ok(res, "OTA status");
    }
}
