package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.CreateIoTDeviceRequest;
import com.isums.assetservice.domains.dtos.IoTDeviceDto;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.entities.IoTDevice;
import com.isums.assetservice.domains.enums.AssetStatus;
import com.isums.assetservice.infrastructures.abstracts.IoTDeviceService;
import com.isums.assetservice.infrastructures.mapper.IoTDeviceMapper;
import com.isums.assetservice.infrastructures.repositories.IoTDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class IoTDeviceServiceImpl implements IoTDeviceService {

    private final IoTDeviceRepository iotDeviceRepository;
    private final IoTDeviceMapper iotDeviceMapper;
    private final DynamoDbClient dynamoDbClient;

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
                iot.getCategoryCode(),
                iot.getDetectionType()
        );
    }

    @Override
    public void upsetToDynamoDB(IoTDevice device) {
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

        String assetId = asset.getId().toString();
        String houseId = asset.getHouseId().toString();
        String categoryCode = asset.getCategory().getCode();
        String detectionType = asset.getCategory().getDetectionType().name();
        String areaId = asset.getFunctionAreaId().toString();
        AssetStatus status = asset.getStatus();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("thing", AttributeValue.builder().s(thing).build());
        item.put("assetId", AttributeValue.builder().s(assetId).build());
        item.put("areaId", AttributeValue.builder().s(areaId).build());
        item.put("categoryCode", AttributeValue.builder().s(categoryCode).build());
        item.put("detectionType", AttributeValue.builder().s(detectionType).build());
        item.put("status", AttributeValue.builder().s("PENDING").build());
        item.put("updatedAt", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build());

        if (houseId != null) {
            item.put("houseId", AttributeValue.builder().s(houseId).build());
        }

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build());

    }

    @Override
    public void createIoTDevice(CreateIoTDeviceRequest request) {
        IoTDevice device = IoTDevice.builder()
                .thing(request.thing())
                .serialNumber(request.serialNumber())
                .assetItem(request.assetItem())
                .build();

        var savedDevice = iotDeviceRepository.save(device);
        upsetToDynamoDB(savedDevice);
    }
}
