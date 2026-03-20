package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ActivatedRequest;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.infrastructures.abstracts.IotProvisioningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/iot")
@RequiredArgsConstructor
public class InternalIotController {

    private final IotProvisioningService provisioningService;

    @PostMapping("/activated")
    public ApiResponse<Void> activated(@RequestBody @Valid ActivatedRequest req) {
        provisioningService.activateController(req.thingName());
        return ApiResponses.noContent();
    }
}
