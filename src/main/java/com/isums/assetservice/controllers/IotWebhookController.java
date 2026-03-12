package com.isums.assetservice.controllers;

import com.isums.assetservice.infrastructures.abstracts.IotProvisioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/iot")
@RequiredArgsConstructor
public class IotWebhookController {

    private final IotProvisioningService provisioningService;

    @PostMapping("/activated")
    public void activated(@RequestBody Map<String, String> body) {
        String thingName = body.get("thingName");
        if (thingName != null) {
            provisioningService.activateController(thingName);
        }
    }
}
