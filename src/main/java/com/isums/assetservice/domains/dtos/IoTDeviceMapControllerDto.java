package com.isums.assetservice.domains.dtos;

import com.isums.assetservice.domains.enums.AssetStatus;
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
    AssetStatus status;
}
