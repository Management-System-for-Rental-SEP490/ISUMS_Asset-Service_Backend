package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.CreateIoTDeviceRequest;
import com.isums.assetservice.domains.dtos.IoTDeviceDto;
import com.isums.assetservice.domains.dtos.IoTDeviceMapControllerDto;
import com.isums.assetservice.domains.dtos.IotControllerDto;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.entities.IoTDevice;
import com.isums.assetservice.domains.entities.IotController;
import com.isums.assetservice.exceptions.NotFoundException;
import com.isums.assetservice.infrastructures.abstracts.IoTDeviceService;
import com.isums.assetservice.infrastructures.grpcs.HouseGrpcImpl;
import com.isums.assetservice.infrastructures.mapper.IoTControllerMapper;
import com.isums.assetservice.infrastructures.mapper.IoTDeviceMapper;
import com.isums.assetservice.infrastructures.repositories.IoTDeviceRepository;
import com.isums.assetservice.infrastructures.repositories.IotControllerRepository;
import com.isums.houseservice.grpc.FunctionalAreaResponse;
import com.isums.houseservice.grpc.HouseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IoTDeviceServiceImpl implements IoTDeviceService {

    private final IoTDeviceRepository iotDeviceRepository;
    private final DynamoDbClient dynamoDbClient;
    private final IotControllerRepository iotControllerRepository;
    private final IoTControllerMapper ioTControllerMapper;
    private final IoTDeviceMapper ioTDeviceMapper;
    private final HouseGrpcImpl houseGrpc;

    @Value("${app.ddb.assetMapTable}")
    private String tableName;

    @Override
    public IoTDeviceDto getByThing(String thing) {

        var iot = iotDeviceRepository.findViewByThing(thing)
                .orElseThrow(() -> new RuntimeException("IoT device not found for thing=" + thing));

        return new IoTDeviceDto(
                iot.getIotDeviceId(),
                iot.getThing(),
                iot.getSerialNumber(),
                iot.getAssetId(),
                iot.getHouseId(),
                iot.getAreaId(),
                iot.getCategoryId(),
                iot.getCategoryCode()
        );
    }

    @Override
    public void upsetToDynamoDB(IoTDevice device, String areaName) {
        String thing = device.getThing();
        if (thing == null || thing.isBlank()) {
            log.error("Thing is empty");
            return;
        }

        AssetItem asset = device.getAssetItem();
        if (asset == null) {
            log.error("Asset is empty");
            return;
        }

        UUID houseUuid = asset.getHouseId();
        String houseId = houseUuid.toString();
        String categoryCode = asset.getCategory().getCode();
        UUID areaId = asset.getFunctionAreaId();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("thing", av(thing));
        item.put("houseId", av(houseId));
        item.put("role", av("NODE"));
        item.put("assetId", av(asset.getId().toString()));
        item.put("categoryCode", av(categoryCode));
        item.put("status", av("ACTIVE"));
        item.put("updatedAt", avn(String.valueOf(System.currentTimeMillis())));

        if (areaId != null) {
            item.put("areaId", av(areaId.toString()));
        }
        if (areaName != null) {
            item.put("areaName", av(areaName));
        }

        // Denormalise the tenant user id so esp32-threshold-checker /
        // esp32-eif-score can route multi-channel notifications without a
        // second service hop. HouseResponse.user_rental_id is the current
        // renter despite the name suggesting otherwise (see house.proto).
        // When the house has no active tenant yet the Lambda silently
        // skips external channels (push WS still fires).
        try {
            HouseResponse house = houseGrpc.getHouseById(houseUuid);
            if (house.getUserRentalId() != null && !house.getUserRentalId().isBlank()) {
                item.put("tenantUserId", av(house.getUserRentalId()));
            }
        } catch (Exception e) {
            log.warn("Failed to resolve tenantUserId houseId={}: {}", houseId, e.getMessage());
        }

        Set<String> capabilities = device.getCapabilities();
        if (capabilities != null && !capabilities.isEmpty()) {
            item.put("capabilities", AttributeValue.builder()
                    .ss(new ArrayList<>(capabilities))
                    .build());
        }

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build());

        log.info("Synced node {} to DynamoDB houseId={} areaId={} tenantPresent={}",
                thing, houseId, areaId, item.containsKey("tenantUserId"));
    }

    private AttributeValue av(String v) {
        return AttributeValue.builder().s(v).build();
    }

    private AttributeValue avn(String v) {
        return AttributeValue.builder().n(v).build();
    }

    @Override
    public void createIoTDevice(CreateIoTDeviceRequest request) {
        IoTDevice device = IoTDevice.builder()
                .thing(request.thing())
                .serialNumber(request.serialNumber())
                .assetItem(request.assetItem())
                .build();

        var savedDevice = iotDeviceRepository.save(device);
        upsetToDynamoDB(savedDevice, request.areaName());
    }

    @Override
    @Transactional(readOnly = true)
//    @Cacheable(value = "allIoT", key = "#houseId")
    public IotControllerDto getAllIotByHouse(UUID houseId) {
        IotController controller = iotControllerRepository.findByHouseId(houseId)
                .orElseThrow(() -> new NotFoundException("IoT controller not found for house: " + houseId));

        HouseResponse house = houseGrpc.getHouseById(houseId);
        log.info("=== got house: {}", house.getName());

        String areaName = null;
        if (controller.getAreaId() != null) {
            areaName = house.getFunctionalAreasList().stream().filter(a -> a.getId()
                            .equals(controller.getAreaId().toString()))
                    .map(FunctionalAreaResponse::getName).findFirst().orElse(null);
        }

        IotControllerDto controllerDto = ioTControllerMapper.toIotControllerDto(controller);
        log.info("=== mapped controller");
        controllerDto.setAreaName(areaName);
        controllerDto.setHouseName(house.getName());

        Map<String, String> areaNameMap = house.getFunctionalAreasList().stream()
                .collect(Collectors.toMap(
                        FunctionalAreaResponse::getId,
                        FunctionalAreaResponse::getName
                ));

        List<IoTDevice> devices = iotDeviceRepository.findByAssetItem_HouseId(houseId);
        log.info("=== got devices: {}", devices.size());

        List<IoTDeviceMapControllerDto> deviceDtos = ioTDeviceMapper.toIoTDeviceMapControllerDtoList(devices, areaNameMap);
        log.info("=== mapped devices");
        controllerDto.setDevices(deviceDtos);

        return controllerDto;
    }
}
