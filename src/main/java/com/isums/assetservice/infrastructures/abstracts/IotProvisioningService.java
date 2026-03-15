package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.dtos.IotProvisionResponse;
import com.isums.assetservice.domains.dtos.ControllerInfoResponse;
import com.isums.assetservice.domains.dtos.NodeProvisionResponse;

import java.util.Set;
import java.util.UUID;

public interface IotProvisioningService {

    IotProvisionResponse provisionController(UUID houseId, UUID areaId, String deviceId);

    void activateController(String thingName);

    void deprovisionController(UUID houseId);

    ControllerInfoResponse getControllerByHouse(UUID houseId);

    void assignNodeToArea(String thing, UUID areaId);

    NodeProvisionResponse provisionNode(UUID houseId, String serial, String token, UUID areaId);

    void updateNodeCapabilities(String thing, Set<String> capabilities);
}
