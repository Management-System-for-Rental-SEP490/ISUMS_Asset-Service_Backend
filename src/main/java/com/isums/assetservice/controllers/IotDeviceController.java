package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.IoTDeviceDto;
import com.isums.assetservice.domains.dtos.IotControllerDto;
import com.isums.assetservice.infrastructures.abstracts.IoTDeviceService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/assets/iot-devices")
@RequiredArgsConstructor
public class IotDeviceController {

    private final IoTDeviceService iotDeviceService;

    @GetMapping("/{thing}")
    public ApiResponse<IoTDeviceDto> getByThing(@PathVariable @NotBlank String thing) {
        var res = iotDeviceService.getByThing(thing);
        return ApiResponses.ok(res, "Success to get IoT device");
    }

    @GetMapping("/house/{houseId}")
    public ApiResponse<IotControllerDto> getAllIotDevices(@PathVariable UUID houseId) {
        var res = iotDeviceService.getAllIotByHouse(houseId);
        return ApiResponses.ok(res, "Success to get IoT devices");
    }
}
