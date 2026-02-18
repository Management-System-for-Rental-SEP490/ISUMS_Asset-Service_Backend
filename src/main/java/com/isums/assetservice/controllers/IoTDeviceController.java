package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.IoTDeviceDto;
import com.isums.assetservice.infrastructures.abstracts.IoTDeviceService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/iot-devices")
@RequiredArgsConstructor
public class IoTDeviceController {
    private final IoTDeviceService iotDeviceService;

    @GetMapping("/{thing}")
    public ApiResponse<IoTDeviceDto> getByThing(@PathVariable @NotBlank String thing) {
        var res = iotDeviceService.getByThing(thing);
        return ApiResponses.ok(res, "Success to get IoT device");
    }
}
