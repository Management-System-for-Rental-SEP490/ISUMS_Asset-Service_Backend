package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.dtos.*;
import com.isums.assetservice.domains.enums.Severity;

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

    PagedResponse<AlertDto> getAlerts(UUID houseId, int limit, String cursor, String date, Severity level);

    void sendCommand(UUID houseId, IotCommandRequest req);

    OtaUploadUrlResponse getOtaUploadUrl(UUID houseId, String filename);

    OtaJobResponse triggerOta(UUID houseId, OtaRequest req);

    OtaStatusResponse getOtaStatus(UUID houseId, String jobId);
}
