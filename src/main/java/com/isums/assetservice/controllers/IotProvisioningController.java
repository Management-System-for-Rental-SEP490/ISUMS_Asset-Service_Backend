package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.IotProvisionResponse;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.ControllerInfoResponse;
import com.isums.assetservice.domains.dtos.IotProvisionRequest;
import com.isums.assetservice.infrastructures.abstracts.IotProvisioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/assets/houses/{houseId}/iot")
@RequiredArgsConstructor
public class IotProvisioningController {

    private final IotProvisioningService iotProvisioningService;

    @PostMapping("/provision")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('MANAGER')")
    public ApiResponse<IotProvisionResponse> provision(@PathVariable UUID houseId, @RequestBody IotProvisionRequest req) {
        var res = iotProvisioningService.provisionController(houseId, req.deviceId());
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
}
