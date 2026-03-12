package com.isums.assetservice.domains.dtos;

import com.isums.assetservice.domains.entities.AssetItem;

import java.util.UUID;

public record CreateIoTDeviceRequest(
        String thing,
        String serialNumber,
        AssetItem assetItem
) {
}
