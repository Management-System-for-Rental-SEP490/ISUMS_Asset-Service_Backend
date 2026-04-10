package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.*;
import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.entities.IoTDevice;
import com.isums.assetservice.domains.entities.IotController;
import com.isums.assetservice.domains.enums.AssetStatus;
import com.isums.assetservice.domains.enums.IotControllerStatus;
import com.isums.assetservice.domains.enums.NodeCapability;
import com.isums.assetservice.domains.enums.Severity;
import com.isums.assetservice.exceptions.ConflictException;
import com.isums.assetservice.infrastructures.abstracts.IotNodeTokenService;
import com.isums.assetservice.infrastructures.abstracts.IotProvisioningService;
import com.isums.assetservice.infrastructures.grpcs.HouseGrpcImpl;
import com.isums.assetservice.infrastructures.repositories.AssetCategoryRepository;
import com.isums.assetservice.infrastructures.repositories.AssetItemRepository;
import com.isums.assetservice.infrastructures.repositories.IoTDeviceRepository;
import com.isums.assetservice.infrastructures.repositories.IotControllerRepository;
import com.isums.houseservice.grpc.FunctionalAreaResponse;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CertificateStatus;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

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
    private final HouseGrpcImpl houseGrpc;
    private final ObjectMapper objectMapper;
    private final S3Presigner s3Presigner;
    private final IotDataPlaneClient iotDataPlaneClient;

    @Value("${app.iot.policy-name}")
    private String policyName;

    @Value("${app.iot.mqtt-endpoint}")
    private String mqttEndpoint;

    @Value("${app.ddb.assetMapTable}")
    private String assetMapTable;

    @Value("${app.ddb.alertsTable}")
    private String alertsTable;

    @Value("${app.iot.ota-bucket}")
    private String otaBucket;

    @Value("${app.ddb.otaJobsTable:iot-ota-jobs}")
    private String otaJobsTable;

    @Override
    public IotProvisionResponse provisionController(UUID houseId, UUID areaId, String deviceId) {
        var deviceExisted = controllerRepository.findByDeviceId(deviceId);
        if (deviceExisted.isPresent()) {
            var ctrl = deviceExisted.get();
            log.info("Device {} already provisioned for house {}", deviceId, ctrl.getHouseId());
            if (ctrl.getStatus() != IotControllerStatus.DEPROVISIONED) {
                throw new ConflictException(
                        "Device " + deviceId + " already provisioned for house " + ctrl.getHouseId());
            }
        }

        String thingName = "ctrl-" + deviceId.replace(":", "").toLowerCase();
        try {
            iotClient.createThing(r -> r.thingName(thingName));
            log.info("Created thing {}", thingName);

            var certResult = iotClient.createKeysAndCertificate(r -> r.setAsActive(true));
            log.info("Created certificate for thing {}", thingName);

            iotClient.attachPolicy(r -> r.policyName(policyName).target(certResult.certificateArn()));

            iotClient.attachThingPrincipal(r -> r.thingName(thingName).principal(certResult.certificateArn()));

            IotController controller = deviceExisted
                    .filter(c -> c.getStatus() == IotControllerStatus.DEPROVISIONED)
                    .orElse(IotController.builder().build());

            controller.setDeviceId(deviceId);
            controller.setHouseId(houseId);
            controller.setAreaId(areaId);
            controller.setThingName(thingName);
            controller.setCertificateArn(certResult.certificateArn());
            controller.setStatus(IotControllerStatus.PENDING);
            controllerRepository.save(controller);

            syncControllerToDynamoDB(thingName, houseId, areaId);

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

    private void syncControllerToDynamoDB(String thingName, UUID houseId, UUID areaId) {
        String areaName = getAreaName(houseId, areaId);

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("thing", AttributeValue.builder().s(thingName).build());
        item.put("houseId", AttributeValue.builder().s(houseId.toString()).build());
        item.put("role", AttributeValue.builder().s("CONTROLLER").build());
        item.put("status", AttributeValue.builder().s("PENDING").build());
        item.put("updatedAt", AttributeValue.builder()
                .n(String.valueOf(System.currentTimeMillis())).build());

        if (areaId != null) item.put("areaId", AttributeValue.builder().s(areaId.toString()).build());
        if (areaName != null) item.put("areaName", AttributeValue.builder().s(areaName).build());

        try {
            dynamoDbClient.putItem(r -> r.tableName(assetMapTable).item(item));
            log.info("Synced controller {} to DynamoDB areaId={} areaName={}", thingName, areaId, areaName);
        } catch (Exception e) {
            log.error("DynamoDB sync failed for {}: {}", thingName, e.getMessage());
        }
    }

    @Override
    public void activateController(String thingName) {
        controllerRepository.findByThingName(thingName).ifPresentOrElse(ctrl -> {
            if (ctrl.getStatus() == IotControllerStatus.ACTIVE) {
                log.info("Controller {} already active", thingName);
                return;
            }
            ctrl.setStatus(IotControllerStatus.ACTIVE);
            ctrl.setActivatedAt(Instant.now());
            controllerRepository.save(ctrl);

            dynamoDbClient.updateItem(r -> r
                    .tableName(assetMapTable)
                    .key(Map.of("thing", AttributeValue.builder().s(thingName).build()))
                    .attributeUpdates(Map.of(
                            "status", AttributeValueUpdate.builder()
                                    .value(AttributeValue.builder().s("ACTIVE").build())
                                    .action(AttributeAction.PUT).build(),
                            "activatedAt", AttributeValueUpdate.builder()
                                    .value(AttributeValue.builder()
                                            .n(String.valueOf(System.currentTimeMillis())).build())
                                    .action(AttributeAction.PUT).build()
                    ))
            );
            log.info("Activated controller {}", thingName);
        }, () -> log.warn("Controller not found: {}", thingName));
    }

    @Override
    public void deprovisionController(UUID houseId) {
        controllerRepository.findByHouseId(houseId).ifPresent(ctrl -> {
            try {
                iotClient.listThingPrincipals(r -> r.thingName(ctrl.getThingName()))
                        .principals()
                        .forEach(principal -> {
                            try {
                                iotClient.detachThingPrincipal(r -> r.thingName(ctrl.getThingName()).principal(principal));
                                log.info("Detached principal {} from {}", principal, ctrl.getThingName());
                            } catch (Exception e) {
                                log.warn("detachThingPrincipal skipped: {}", e.getMessage());
                            }
                        });
            } catch (Exception e) {
                log.warn("listThingPrincipals skipped: {}", e.getMessage());
            }

            try {
                iotClient.updateCertificate(r -> r.certificateId(extractCertId(ctrl.getCertificateArn())).newStatus(CertificateStatus.INACTIVE));
            } catch (Exception e) {
                log.warn("updateCertificate skipped: {}", e.getMessage());
            }

            try {
                iotClient.deleteCertificate(r -> r.certificateId(extractCertId(ctrl.getCertificateArn())).forceDelete(true));
            } catch (Exception e) {
                log.warn("deleteCertificate skipped: {}", e.getMessage());
            }

            try {
                iotClient.deleteThing(r -> r.thingName(ctrl.getThingName()));
            } catch (Exception e) {
                log.warn("deleteThing skipped: {}", e.getMessage());
            }

            ctrl.setStatus(IotControllerStatus.DEPROVISIONED);
            controllerRepository.save(ctrl);

            try {
                dynamoDbClient.deleteItem(r -> r.tableName(assetMapTable).key(Map.of("thing", AttributeValue.builder().s(ctrl.getThingName()).build())));
            } catch (Exception e) {
                log.warn("DynamoDB deleteItem skipped: {}", e.getMessage());
            }

            log.info("Deprovisioned controller {}", ctrl.getThingName());
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
        UUID houseId = asset.getHouseId();
        asset.setFunctionAreaId(areaId);
        assetItemRepository.save(asset);

        String areaName = getAreaName(houseId, areaId);

        ioTDeviceService.upsetToDynamoDB(device, areaName);

        log.info("Assigned node {} to area {} ({})", thing, areaId, areaName);
    }

    @Override
    @Transactional
    public NodeProvisionResponse provisionNode(UUID houseId, String serial, String token, UUID areaId) {
        if (!iotNodeTokenService.isTokenValid(serial, token)) {
            log.warn("Invalid token for serial={}", serial);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        ioTDeviceRepository.findBySerialNumber(serial).ifPresent(d -> {
            throw new ConflictException("Node " + serial + " already provisioned");
        });

        IotController controller = controllerRepository.findByHouseId(houseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No controller found for house " + houseId +
                                ". Please provision controller first."));

        AssetCategory category = assetCategoryRepository.findByCode("IOT_NODE")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "AssetCategory IOT_NODE not found. Contact admin."));

        try {
            AssetItem assetItem = AssetItem.builder()
                    .houseId(houseId)
                    .functionAreaId(areaId)
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

            String areaName = getAreaName(houseId, areaId);

            ioTDeviceService.upsetToDynamoDB(savedDevice, areaName);

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
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Node provision failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateNodeCapabilities(String thing, Set<NodeCapability> capabilities) {
        IoTDevice device = ioTDeviceRepository.findByThing(thing)
                .orElseThrow(() -> new NotFoundException("IoT device not found: " + thing));

        Set<String> capStrings = capabilities.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        device.setCapabilities(capStrings);
        ioTDeviceRepository.save(device);

        UUID houseId = device.getAssetItem().getHouseId();
        UUID areaId = device.getAssetItem().getFunctionAreaId();
        String areaName = getAreaName(houseId, areaId);

        ioTDeviceService.upsetToDynamoDB(device, areaName);
        log.info("Updated capabilities thing={} caps={}", thing, capabilities);
    }

    @Override
    public PagedResponse<AlertDto> getAlerts(UUID houseId, int limit, String cursor, String date, Severity level) {
        try {
            String dateStr = (date != null && !date.isBlank())
                    ? date
                    : LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString();

            String pk = houseId + "#" + dateStr;

            Map<String, AttributeValue> exprValues = new HashMap<>();
            exprValues.put(":pk", AttributeValue.builder().s(pk).build());

            String filterExpr = null;
            Map<String, String> exprNames = null;
            if (level != null) {
                filterExpr = "#lv = :lv";
                exprNames = Map.of("#lv", "level");
                exprValues.put(":lv", AttributeValue.builder().s(level.toString().toUpperCase()).build());
            }

            QueryRequest.Builder queRequest = QueryRequest.builder()
                    .tableName(alertsTable)
                    .indexName("house-date-ts-index")
                    .keyConditionExpression("houseDatePartition = :pk")
                    .expressionAttributeValues(exprValues)
                    .scanIndexForward(false)
                    .limit(limit);

            if (filterExpr != null) {
                queRequest.filterExpression(filterExpr);
            }

            if (exprNames != null) {
                queRequest.expressionAttributeNames(exprNames);
            }

            if (cursor != null && !cursor.isBlank()) {
                Map<String, AttributeValue> startKey = decodeCursor(cursor);
                if (!startKey.isEmpty()) {
                    queRequest.exclusiveStartKey(startKey);
                }
            }

            QueryResponse queResponse = dynamoDbClient.query(queRequest.build());

            List<AlertDto> items = queResponse.items().stream().map(this::mapToAlertDto).toList();

            String nextCursor = null;
            if (queResponse.lastEvaluatedKey() != null && !queResponse.lastEvaluatedKey().isEmpty()) {
                nextCursor = encodeCursor(queResponse.lastEvaluatedKey());
            }

            log.info("getAlerts: houseId={} date={} level={} count={} hasMore={}", houseId, dateStr, level, items.size(), nextCursor != null);

            return PagedResponse.<AlertDto>builder()
                    .items(items)
                    .nextCursor(nextCursor)
                    .hasMore(nextCursor != null)
                    .build();
        } catch (Exception e) {
            log.error("getAlerts failed houseId={}", houseId, e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get alerts");
        }
    }

    @Override
    public void sendCommand(UUID houseId, IotCommandRequest req) {
        IotController ctrl = controllerRepository.findByHouseId(houseId)
                .orElseThrow(() -> new NotFoundException("No controller for house " + houseId));

        String topic = "esp32/" + ctrl.getThingName() + "/cmd";
        Map<String, Object> payload = new HashMap<>();
        payload.put("cmd", req.cmd());
        if (req.serial() != null && !req.serial().isBlank())
            payload.put("serial", req.serial());

        try {
            iotDataPlaneClient.publish(r -> r.topic(topic).qos(1)
                    .payload(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(payload)))
            );
            log.info("sendCommand houseId={} topic={} cmd={} serial={}",
                    houseId, topic, req.cmd(), req.serial());
        } catch (Exception e) {
            log.error("sendCommand failed houseId={}", houseId, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "MQTT publish failed");
        }
    }

    @Override
    public OtaUploadUrlResponse getOtaUploadUrl(UUID houseId, String filename) {
        String key = "ota/" + houseId + "/" + System.currentTimeMillis() + "_" + filename;
        int expiresIn = 3600;

        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expiresIn))
                .putObjectRequest(r -> r.bucket(otaBucket).key(key)
                        .contentType("application/octet-stream"))
                .build();

        String uploadUrl = s3Presigner.presignPutObject(presignReq).url().toString();
        String downloadUrl = "https://" + otaBucket + ".s3.ap-southeast-1.amazonaws.com/" + key;

        return new OtaUploadUrlResponse(uploadUrl, downloadUrl, expiresIn);
    }

    @Override
    public OtaJobResponse triggerOta(UUID houseId, OtaRequest req) {
        IotController ctrl = controllerRepository.findByHouseId(houseId)
                .orElseThrow(() -> new NotFoundException("No controller for house " + houseId));

        String jobId = "ota-" + UUID.randomUUID();
        String topic = "esp32/" + ctrl.getThingName() + "/cmd";

        Map<String, Object> payload = new HashMap<>();
        payload.put("cmd", "ota_update");
        payload.put("job_id", jobId);
        payload.put("url", req.url());
        payload.put("version", req.version());
        payload.put("target", req.target());
        if (req.serial() != null) payload.put("serial", req.serial());
        if (req.md5() != null) payload.put("md5", req.md5());

        try {
            iotDataPlaneClient.publish(r -> r.topic(topic).qos(1)
                    .payload(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(payload)))
            );
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "MQTT publish failed");
        }

        saveOtaJob(jobId, houseId, ctrl.getThingName(), req);

        return new OtaJobResponse(jobId, ctrl.getThingName(), req.target(),
                req.serial(), req.version(), req.url(),
                Instant.now().toEpochMilli());
    }

    @Override
    public OtaStatusResponse getOtaStatus(UUID houseId, String jobId) {
        try {
            var result = dynamoDbClient.getItem(r -> r
                    .tableName(otaJobsTable)
                    .key(Map.of("jobId", av(jobId)))
            );
            if (!result.hasItem()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "OTA job not found: " + jobId);
            }
            Map<String, AttributeValue> item = result.item();
            return new OtaStatusResponse(
                    jobId,
                    item.getOrDefault("status",   av("PENDING")).s(),
                    item.containsKey("progress")  ? Integer.parseInt(item.get("progress").n()) : null,
                    item.containsKey("error")     ? item.get("error").s() : null,
                    item.containsKey("updatedAt") ? Long.parseLong(item.get("updatedAt").n()) : 0L
            );
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("getOtaStatus failed jobId={}", jobId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get OTA status");
        }
    }

    private String extractCertId(String arn) {
        return arn.substring(arn.lastIndexOf("/") + 1);
    }

    private String getAreaName(UUID houseId, UUID areaId) {
        if (areaId == null) return null;
        try {
            var house = houseGrpc.getHouseById(houseId);
            return house.getFunctionalAreasList().stream()
                    .filter(a -> a.getId().equals(areaId.toString()))
                    .map(FunctionalAreaResponse::getName)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get areaName houseId={} areaId={}: {}", houseId, areaId, e.getMessage());
            return null;
        }
    }

    private Map<String, AttributeValue> decodeCursor(String cursor) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cursor);
            String json = new String(bytes, StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(json);
            Map<String, AttributeValue> result = new HashMap<>();
            root.properties().forEach(entry -> {
                JsonNode value = entry.getValue();
                if (value.has("S")) {
                    result.put(entry.getKey(), AttributeValue.builder().s(value.get("S").asString()).build());
                } else if (value.has(("N"))) {
                    result.put(entry.getKey(), AttributeValue.builder().n(value.get("N").asString()).build());
                }
            });

            return result;
        } catch (Exception e) {
            log.warn("decodeCursor failed: {}", e.getMessage());
            return Map.of();
        }
    }

    private AlertDto mapToAlertDto(Map<String, AttributeValue> item) {
        return AlertDto.builder()
                .alertId(str(item, "alertId"))
                .houseId(str(item, "houseId"))
                .areaId(str(item, "areaId"))
                .areaName(str(item, "areaName"))
                .thing(str(item, "thing"))
                .alertType(str(item, "alertType"))
                .title(str(item, "title"))
                .detail(str(item, "detail"))
                .metric(str(item, "metric"))
                .value(num(item, "value"))
                .level(str(item, "level"))
                .resolved(item.containsKey("resolved") && Boolean.TRUE.equals(item.get("resolved").bool()))
                .ts(numLong(item, "ts"))
                .date(str(item, "date"))
                .build();
    }

    private String encodeCursor(Map<String, AttributeValue> lastKey) {
        try {
            Map<String, Map<String, String>> simple = new HashMap<>();
            lastKey.forEach((k, v) -> {
                Map<String, String> typeVal = new HashMap<>();
                if (v.s() != null) typeVal.put("S", v.s());
                else if (v.n() != null) typeVal.put("N", v.n());
                simple.put(k, typeVal);
            });

            String json = objectMapper.writeValueAsString(simple);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("encodeCursor failed: {}", e.getMessage());
            return null;
        }
    }

    private String str(Map<String, AttributeValue> item, String key) {
        AttributeValue v = item.get(key);
        return (v != null && v.s() != null) ? v.s() : null;
    }

    private Double num(Map<String, AttributeValue> item, String key) {
        AttributeValue v = item.get(key);
        if (v == null || v.n() == null) return null;
        try {
            return Double.parseDouble(v.n());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long numLong(Map<String, AttributeValue> item, String key) {
        AttributeValue v = item.get(key);
        if (v == null || v.n() == null) return null;
        try {
            return Long.parseLong(v.n());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void saveOtaJob(String jobId, UUID houseId, String thingName, OtaRequest req) {
        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("jobId", av(jobId));
            item.put("houseId", av(houseId.toString()));
            item.put("thingName", av(thingName));
            item.put("target", av(req.target()));
            item.put("version", av(req.version()));
            item.put("url", av(req.url()));
            item.put("status", av("PENDING"));
            item.put("progress", avn("0"));
            item.put("createdAt", avn(String.valueOf(Instant.now().toEpochMilli())));
            item.put("updatedAt", avn(String.valueOf(Instant.now().toEpochMilli())));
            if (req.serial() != null) item.put("serial", av(req.serial()));
            if (req.md5() != null) item.put("md5", av(req.md5()));

            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(otaJobsTable)
                    .item(item)
                    .build());

            log.info("OTA job saved jobId={} houseId={} target={}", jobId, houseId, req.target());
        } catch (Exception e) {
            log.warn("saveOtaJob failed jobId={}: {}", jobId, e.getMessage());
        }
    }

    private AttributeValue av(String v) {
        return AttributeValue.builder().s(v).build();
    }

    private AttributeValue avn(String v) {
        return AttributeValue.builder().n(v).build();
    }
}
