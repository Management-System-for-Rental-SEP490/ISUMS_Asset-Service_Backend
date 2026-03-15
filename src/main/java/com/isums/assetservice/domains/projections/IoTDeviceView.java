package com.isums.assetservice.domains.projections;

import java.util.UUID;

public interface IoTDeviceView {
    UUID getIotDeviceId();

    String getThing();

    String getSerialNumber();

    UUID getAssetId();

    UUID getHouseId();

    UUID getAreaId();

    UUID getCategoryId();

    String getCategoryCode();
}
