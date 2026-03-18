package com.isums.assetservice.domains.dtos;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class IoTDeviceMapControllerDto {
    UUID id;
    String thing;
    String displayName;
    String serialNumber;
    UUID assetId;
    String areaName;
    String categoryCode;
}
