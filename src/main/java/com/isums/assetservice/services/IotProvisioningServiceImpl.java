package com.isums.assetservice.services;

import com.isums.assetservice.domains.IotProvisionResponse;
import com.isums.assetservice.domains.dtos.ControllerInfoResponse;
import com.isums.assetservice.domains.entities.IotController;
import com.isums.assetservice.domains.enums.IotControllerStatus;
import com.isums.assetservice.exceptions.ConflictException;
import com.isums.assetservice.infrastructures.abstracts.IotProvisioningService;
import com.isums.assetservice.infrastructures.repositories.IotControllerRepository;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CertificateStatus;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IotProvisioningServiceImpl implements IotProvisioningService {

    private final IotClient iotClient;
    private final IotControllerRepository controllerRepository;

    @Value("${app.iot.policy-name}")
    private String policyName;

    @Value("${app.iot.mqtt-endpoint}")
    private String mqttEndpoint;

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

    @Override
    public void activateController(String thingName) {
        controllerRepository.findByThingName(thingName).ifPresent(ctrl -> {
            ctrl.setStatus(IotControllerStatus.ACTIVE);
            ctrl.setActivatedAt(Instant.now());
            controllerRepository.save(ctrl);
            log.info("Activated controller {}", thingName);
        });
    }

    @Override
    public void deprovisionController(UUID houseId) {
        controllerRepository.findAll().stream()
                .filter(ctrl -> ctrl.getHouseId().equals(houseId))
                .findFirst().ifPresent(ctrl -> {
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
        return controllerRepository.findAll().stream()
                .filter(ctrl -> ctrl.getHouseId().equals(houseId))
                .findFirst().map(ctrl -> new ControllerInfoResponse(ctrl.getThingName(), ctrl.getDeviceId()))
                .orElseThrow(() -> new NotFoundException("Controller not found for house " + houseId));
    }

    private String extractCertId(String arn) {
        return arn.substring(arn.lastIndexOf("/") + 1);
    }
}
