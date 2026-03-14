package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.IotProvisionResponse;
import com.isums.assetservice.domains.dtos.ControllerInfoResponse;

import java.util.UUID;

public interface IotProvisioningService {

    IotProvisionResponse provisionController(UUID houseId, String deviceId);

    void activateController(String thingName);

    void deprovisionController(UUID houseId);

    ControllerInfoResponse getControllerByHouse(UUID houseId);

    void assignNodeToArea(String thing, UUID areaId);
}
