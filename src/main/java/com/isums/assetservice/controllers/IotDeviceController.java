package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.IoTDeviceDto;
import com.isums.assetservice.domains.dtos.IotControllerDto;
import com.isums.assetservice.infrastructures.abstracts.IoTDeviceService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/assets/iot-devices")
@RequiredArgsConstructor
public class IotDeviceController {

    private final IoTDeviceService iotDeviceService;
    private final MessageSource messageSource;

    private String msg(String code) {
        try {
            return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return code;
        }
    }

    @GetMapping("/{thing}")
    public ApiResponse<IoTDeviceDto> getByThing(@PathVariable @NotBlank String thing) {
        var res = iotDeviceService.getByThing(thing);
        return ApiResponses.ok(res, msg("iot.device_get"));
    }

    @GetMapping("/house/{houseId}")
    public ApiResponse<IotControllerDto> getAllIotDevices(@PathVariable UUID houseId) {
        var res = iotDeviceService.getAllIotByHouse(houseId);
        return ApiResponses.ok(res, msg("iot.devices_get"));
    }
}
