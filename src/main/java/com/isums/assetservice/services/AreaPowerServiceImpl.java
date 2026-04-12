package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AreaPowerStateResponse;
import com.isums.assetservice.domains.entities.AreaPowerState;
import com.isums.assetservice.domains.entities.IoTDevice;
import com.isums.assetservice.domains.entities.IotController;
import com.isums.assetservice.domains.enums.IotControllerStatus;
import com.isums.assetservice.domains.enums.NodeCapability;
import com.isums.assetservice.domains.enums.PowerAction;
import com.isums.assetservice.domains.enums.PowerCutReason;
import com.isums.assetservice.exceptions.NotFoundException;
import com.isums.assetservice.infrastructures.abstracts.AreaPowerService;
import com.isums.assetservice.infrastructures.repositories.AreaPowerStateRepository;
import com.isums.assetservice.infrastructures.repositories.IoTDeviceRepository;
import com.isums.assetservice.infrastructures.repositories.IotControllerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AreaPowerServiceImpl implements AreaPowerService {

    private final IotControllerRepository controllerRepository;
    private final IoTDeviceRepository ioTDeviceRepository;
    private final AreaPowerStateRepository areaPowerStateRepository;
    private final IotDataPlaneClient iotDataPlaneClient;
    private final ObjectMapper objectMapper;

    private static final String MQTT_CMD_POWER_OFF = "relay_off";
    private static final String MQTT_CMD_POWER_ON  = "relay_on";

    @Override
    @Transactional
    public AreaPowerStateResponse toggleAreaPower(UUID houseId, UUID areaId, PowerAction action, UUID requesterId) {
        IotController ctrl = getActiveController(houseId);

        if (ctrl.isPaymentCutActive()) {
            log.warn("[AreaPower] toggle blocked — paymentCutActive houseId={} areaId={} by={}",
                    houseId, areaId, requesterId);
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Power is locked due to overdue payment. Please settle your dues before controlling power.");
        }

        List<IoTDevice> relayNodes = ioTDeviceRepository.findByAreaIdAndCapability(areaId, NodeCapability.RELAY.name());

        if (relayNodes.isEmpty()) {
            throw new NotFoundException("No RELAY-capable devices found in area "
                    + areaId + ". Ensure a node with RELAY capability is assigned to this area.");
        }

        String mqttCmd = (action == PowerAction.OFF) ? MQTT_CMD_POWER_OFF : MQTT_CMD_POWER_ON;
        String topic   = "esp32/" + ctrl.getThingName() + "/cmd";

        int successCount = sendRelayCommands(topic, mqttCmd, relayNodes, houseId, areaId);
        if (successCount == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to send relay command to any device in area " + areaId);
        }

        AreaPowerState state = areaPowerStateRepository.findByHouseIdAndAreaId(houseId, areaId)
                .orElse(AreaPowerState.builder().houseId(houseId).areaId(areaId).build());

        state.setPowered(action == PowerAction.ON);
        state.setCutReason(action == PowerAction.OFF ? PowerCutReason.MANUAL : null);
        state.setPowerCutJobId(null);
        state.setChangedBy(requesterId);
        state.setChangedAt(Instant.now());
        AreaPowerState saved = areaPowerStateRepository.save(state);

        log.info("[AreaPower] toggled houseId={} areaId={} action={} nodes={}/{} by={}",
                houseId, areaId, action, successCount, relayNodes.size(), requesterId);

        return buildResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AreaPowerStateResponse getAreaPowerState(UUID houseId, UUID areaId) {
        IotController ctrl = controllerRepository.findByHouseId(houseId)
                .orElseThrow(() -> new NotFoundException("No IoT controller found for house " + houseId));

        if (ctrl.isPaymentCutActive()) {
            return new AreaPowerStateResponse(
                    houseId, areaId, false,
                    PowerCutReason.PAYMENT_DUE,
                    "Power is locked due to overdue payment. Please settle your dues to restore power.",
                    Instant.now()
            );
        }

        return areaPowerStateRepository.findByHouseIdAndAreaId(houseId, areaId)
                .map(this::buildResponse)
                .orElseGet(() -> new AreaPowerStateResponse(
                        houseId, areaId, true, null,
                        "Power is on", null
                ));
    }

    private IotController getActiveController(UUID houseId) {
        IotController ctrl = controllerRepository.findByHouseId(houseId)
                .orElseThrow(() -> new NotFoundException(
                        "No IoT controller found for house " + houseId));

        if (ctrl.getStatus() != IotControllerStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "IoT controller is not active (status: " + ctrl.getStatus()
                            + "). Please ensure the device is online.");
        }
        return ctrl;
    }

    private int sendRelayCommands(String topic, String cmd, List<IoTDevice> nodes, UUID houseId, UUID areaId) {
        int success = 0;
        for (IoTDevice node : nodes) {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("cmd", cmd);
                payload.put("serial", node.getSerialNumber());

                String json = objectMapper.writeValueAsString(payload);
                iotDataPlaneClient.publish(r -> r
                        .topic(topic)
                        .qos(1)
                        .payload(SdkBytes.fromUtf8String(json))
                );
                success++;
                log.info("[AreaPower] MQTT sent cmd={} serial={} topic={}", cmd, node.getSerialNumber(), topic);
            } catch (Exception e) {
                log.error("[AreaPower] MQTT failed cmd={} serial={} houseId={} areaId={}: {}",
                        cmd, node.getSerialNumber(), houseId, areaId, e.getMessage());
            }
        }
        return success;
    }

    private AreaPowerStateResponse buildResponse(AreaPowerState state) {
        String message;
        if (state.isPowered()) {
            message = "Power is on";
        } else if (state.getCutReason() == PowerCutReason.PAYMENT_DUE) {
            message = "Power is locked due to overdue payment. Please settle your dues to restore power.";
        } else if (state.getCutReason() == PowerCutReason.MANUAL) {
            message = "Power was manually turned off";
        } else {
            message = "Power is off";
        }

        return new AreaPowerStateResponse(
                state.getHouseId(),
                state.getAreaId(),
                state.isPowered(),
                state.getCutReason(),
                message,
                state.getChangedAt()
        );
    }
}