package com.isums.assetservice.services;

import com.isums.assetservice.domains.IotProvisionResponse;
import com.isums.assetservice.domains.dtos.ControllerInfoResponse;
import com.isums.assetservice.domains.dtos.NodeProvisionResponse;
import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.entities.IoTDevice;
import com.isums.assetservice.domains.entities.IotController;
import com.isums.assetservice.domains.enums.AssetStatus;
import com.isums.assetservice.domains.enums.IotControllerStatus;
import com.isums.assetservice.exceptions.ConflictException;
import com.isums.assetservice.infrastructures.abstracts.IotNodeTokenService;
import com.isums.assetservice.infrastructures.abstracts.IotProvisioningService;
import com.isums.assetservice.infrastructures.repositories.AssetCategoryRepository;
import com.isums.assetservice.infrastructures.repositories.AssetItemRepository;
import com.isums.assetservice.infrastructures.repositories.IoTDeviceRepository;
import com.isums.assetservice.infrastructures.repositories.IotControllerRepository;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CertificateStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IotProvisioningServiceImpl implements IotProvisioningService {

    private final IotClient iotClient;
    private final IotControllerRepository controllerRepository;
    private final DynamoDbClient dynamoDbClient;
    private final IoTDeviceRepository ioTDeviceRepository;
    private final AssetItemRepository assetItemRepository;
    private final IoTDeviceServiceImpl ioTDeviceService;
    private final IotNodeTokenService iotNodeTokenService;
    private final AssetCategoryRepository assetCategoryRepository;

    @Value("${app.iot.policy-name}")
    private String policyName;

    @Value("${app.iot.mqtt-endpoint}")
    private String mqttEndpoint;

    @Value("${app.ddb.assetMapTable}")
    private String assetMapTable;

    @Override
    public IotProvisionResponse provisionController(UUID houseId, String deviceId) {
        var deviceExisted = controllerRepository.findByDeviceId(deviceId);
        if (deviceExisted.isPresent()) {
            var ctrl = deviceExisted.get();
            log.info("Device {} already provisioned for house {}", deviceId, ctrl.getHouseId());
            throw new ConflictException(
                    "Device " + deviceId + " already provisioned for house " + ctrl.getHouseId());
        }

        String thingName = "ctrl-" + deviceId.replace(":", "").toLowerCase();
        try {
            iotClient.createThing(r -> r.thingName(thingName));
            log.info("Created thing {}", thingName);

            var certResult = iotClient.createKeysAndCertificate(r -> r.setAsActive(true));
            log.info("Created certificate for thing {}", thingName);

            iotClient.attachPolicy(r -> r.policyName(policyName).target(certResult.certificateArn()));

            iotClient.attachThingPrincipal(r -> r.thingName(thingName).principal(certResult.certificateArn()));

            IotController controller = IotController.builder()
                    .deviceId(deviceId)
                    .houseId(houseId)
                    .thingName(thingName)
                    .certificateArn(certResult.certificateArn())
                    .status(IotControllerStatus.PENDING)
                    .build();
            controllerRepository.save(controller);

            syncControllerToDynamoDB(thingName, houseId);

            return new IotProvisionResponse(
                    thingName,
                    certResult.certificatePem(),
                    certResult.keyPair().privateKey(),
                    mqttEndpoint,
                    houseId.toString()
            );
        } catch (Exception e) {
            try {
                iotClient.deleteThing(r -> r.thingName(thingName));
            } catch (Exception ignored) {
            }
            log.error("Provision failed for device={} house={}", deviceId, houseId, e);
            throw new IllegalStateException("IoT provision failed: " + e.getMessage());
        }
    }

    private void syncControllerToDynamoDB(String thingName, UUID houseId) {
        try {
            dynamoDbClient.putItem(r -> r
                    .tableName(assetMapTable)
                    .item(Map.of(
                            "thing",     AttributeValue.builder().s(thingName).build(),
                            "houseId",   AttributeValue.builder().s(houseId.toString()).build(),
                            "role",      AttributeValue.builder().s("CONTROLLER").build(),
                            "status",    AttributeValue.builder().s("PENDING").build(),
                            "updatedAt", AttributeValue.builder()
                                    .n(String.valueOf(System.currentTimeMillis())).build()
                    ))
            );
            log.info("Synced controller {} to DynamoDB", thingName);
        } catch (Exception e) {
            log.error("DynamoDB sync failed for {}: {}", thingName, e.getMessage());
        }
    }

    @Override
    public void activateController(String thingName) {
        controllerRepository.findByThingName(thingName).ifPresent(ctrl -> {
            if(ctrl.getStatus() != IotControllerStatus.PENDING) return;

            ctrl.setStatus(IotControllerStatus.ACTIVE);
            ctrl.setActivatedAt(Instant.now());
            controllerRepository.save(ctrl);

            dynamoDbClient.updateItem(r -> r
                    .tableName(assetMapTable)
                    .key(Map.of("thing", AttributeValue.builder().s(thingName).build()))
                    .attributeUpdates(Map.of(
                            "status", AttributeValueUpdate.builder()
                                    .value(AttributeValue.builder().s("ACTIVE").build())
                                    .action(AttributeAction.PUT)
                                    .build(),
                            "activatedAt", AttributeValueUpdate.builder()
                                    .value(AttributeValue.builder().n(
                                            String.valueOf(System.currentTimeMillis())).build())
                                    .action(AttributeAction.PUT)
                                    .build()
                    ))
            );
            log.info("Activated controller {} → DynamoDB synced", thingName);
        });
    }

    @Override
    public void deprovisionController(UUID houseId) {
        controllerRepository.findByHouseId(houseId).ifPresent(ctrl -> {
                    try {
                        iotClient.detachThingPrincipal(r -> r.thingName(ctrl.getThingName()).principal(ctrl.getCertificateArn()));
                        iotClient.updateCertificate(r -> r.certificateId(extractCertId(ctrl.getCertificateArn())).newStatus(CertificateStatus.INACTIVE));
                        iotClient.deleteCertificate(r -> r.certificateId(extractCertId(ctrl.getCertificateArn())).forceDelete(true));
                        iotClient.deleteThing(r -> r.thingName(ctrl.getThingName()));

                        ctrl.setStatus(IotControllerStatus.DEPROVISIONED);
                        controllerRepository.save(ctrl);
                        log.info("Deprovisioned controller {}", ctrl.getThingName());
                    } catch (Exception e) {
                        log.error("Deprovision failed: {}", ctrl.getThingName(), e);
                    }
                });
    }

    @Override
    public ControllerInfoResponse getControllerByHouse(UUID houseId) {
        return controllerRepository.findByHouseId(houseId).map(ctrl -> new ControllerInfoResponse(ctrl.getThingName(), ctrl.getDeviceId()))
                .orElseThrow(() -> new NotFoundException("Controller not found for house " + houseId));
    }

    @Override
    public void assignNodeToArea(String thing, UUID areaId) {
        IoTDevice device = ioTDeviceRepository.findByThing(thing)
                .orElseThrow(() -> new NotFoundException("IoT device not found: " + thing));

        AssetItem asset = device.getAssetItem();
        asset.setFunctionAreaId(areaId);
        assetItemRepository.save(asset);

        ioTDeviceService.upsetToDynamoDB(device);

        log.info("Assigned node {} to area {}", thing, areaId);
    }

    @Override
    @Transactional
    public NodeProvisionResponse provisionNode(UUID houseId, String serial, String token) {
        if (!iotNodeTokenService.isTokenValid(serial, token)) {
            log.warn("Invalid token for serial={}", serial);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        ioTDeviceRepository.findBySerialNumber(serial).ifPresent(d -> {
            throw new ConflictException("Node " + serial + " already provisioned");
        });

        IotController controller = controllerRepository.findByHouseId(houseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No controller found for house " + houseId + ". Provision controller first."));

        if (controller.getStatus() != IotControllerStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Controller not activated yet. Wait for controller to connect.");
        }

        AssetCategory category = assetCategoryRepository.findByCode("IOT_NODE")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AssetCategory IOT_NODE not found. Contact admin."));

        try {
            AssetItem assetItem = AssetItem.builder()
                    .houseId(houseId)
                    .functionAreaId(null)
                    .category(category)
                    .displayName("Node " + serial.substring(Math.max(0, serial.length() - 8)))
                    .serialNumber(serial)
                    .conditionPercent(100)
                    .status(AssetStatus.IN_USE)
                    .build();
            AssetItem savedAsset = assetItemRepository.save(assetItem);

            IoTDevice device = IoTDevice.builder()
                    .thing(serial)
                    .serialNumber(serial)
                    .assetItem(savedAsset)
                    .build();
            IoTDevice savedDevice = ioTDeviceRepository.save(device);

            ioTDeviceService.upsetToDynamoDB(savedDevice);

            iotNodeTokenService.revokeToken(serial);

            log.info("Provisioned node serial={} houseId={} assetId={}",
                    serial, houseId, savedAsset.getId());

            return new NodeProvisionResponse(
                    serial,
                    controller.getDeviceId(),
                    houseId.toString(),
                    savedAsset.getId().toString()
            );

        } catch (ConflictException | ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("provisionNode failed serial={} houseId={}", serial, houseId, e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Node provision failed: " + e.getMessage());
        }
    }

    private String extractCertId(String arn) {
        return arn.substring(arn.lastIndexOf("/") + 1);
    }
}
