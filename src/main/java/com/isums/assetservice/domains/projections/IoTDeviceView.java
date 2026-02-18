package com.isums.assetservice.domains.projections;

import com.isums.assetservice.domains.enums.DetectionType;

import java.util.UUID;

public interface IoTDeviceView {
    UUID getIotDeviceId();
    String getThing();
    String getSerialNumber();

    UUID getAssetId();
    UUID getHouseId();

    UUID getCategoryId();
    String getCategoryCode();
    DetectionType getDetectionType();
}
