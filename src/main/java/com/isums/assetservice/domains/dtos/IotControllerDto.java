package com.isums.assetservice.domains.dtos;

import com.isums.assetservice.domains.enums.IotControllerStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class IotControllerDto {
    UUID id;
    String deviceId;
    String houseName;
    String areaName;
    String thingName;
    IotControllerStatus status;
    Instant createdAt;
    Instant activatedAt;
    List<IoTDeviceMapControllerDto> devices;
}
