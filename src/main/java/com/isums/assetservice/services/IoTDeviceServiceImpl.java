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

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IoTDeviceServiceImpl implements IoTDeviceService {

    private final IoTDeviceRepository iotDeviceRepository;
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
                iot.getCategoryCode()
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

        String houseId      = asset.getHouseId().toString();
        String categoryCode = asset.getCategory().getCode();
        UUID areaId     = asset.getFunctionAreaId();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("thing",        av(thing));
        item.put("houseId",      av(houseId));
        item.put("role",         av("NODE"));
        item.put("assetId",      av(asset.getId().toString()));
        item.put("categoryCode", av(categoryCode));
        item.put("status",       av("ACTIVE"));
        item.put("updatedAt",    avn(String.valueOf(System.currentTimeMillis())));

        if (areaId != null) {
            item.put("areaId", av(areaId.toString()));
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

        log.info("Synced node {} to DynamoDB houseId={} areaId={}", thing, houseId, areaId);
    }

    private AttributeValue av(String v)  { return AttributeValue.builder().s(v).build(); }
    private AttributeValue avn(String v) { return AttributeValue.builder().n(v).build(); }

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
