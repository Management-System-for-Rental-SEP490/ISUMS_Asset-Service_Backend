package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.dtos.*;
import com.isums.assetservice.domains.enums.NodeCapability;
import com.isums.assetservice.domains.enums.Severity;

import java.util.Set;
import java.util.UUID;

public interface IotProvisioningService {

    IotProvisionResponse provisionController(UUID houseId, UUID areaId, String deviceId);

    void activateController(String thingName);

    void deprovisionController(UUID houseId);

    ControllerInfoResponse getControllerByHouse(UUID houseId);

    void assignNodeToArea(String thing, UUID areaId);

    NodeProvisionResponse provisionNode(UUID houseId, String serial, String token,
                                        UUID areaId, Set<NodeCapability> capabilities);

    void updateNodeCapabilities(String thing, Set<NodeCapability> capabilities);

    /**
     * Safety net cho Lambda: chỉ set capabilities khi device chưa có.
     * Không ghi đè nếu đã được set lúc provision.
     */
    void syncNodeCapabilitiesIfEmpty(String thing, Set<String> capabilities);

    PagedResponse<AlertDto> getAlerts(UUID houseId, int limit, String cursor, String date, Severity level);

    void sendCommand(UUID houseId, IotCommandRequest req);

    OtaUploadUrlResponse getOtaUploadUrl(UUID houseId, String filename);

    OtaJobResponse triggerOta(UUID houseId, OtaRequest req);

    OtaStatusResponse getOtaStatus(UUID houseId, String jobId);

    void sendPowerCutCommand(UUID houseId);
}