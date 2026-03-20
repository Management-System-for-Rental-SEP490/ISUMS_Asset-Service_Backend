package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.ThresholdRequest;
import com.isums.assetservice.domains.dtos.ThresholdResponse;
import com.isums.assetservice.domains.entities.IotThreshold;
import com.isums.assetservice.domains.enums.Severity;
import com.isums.assetservice.infrastructures.mapper.IotThresholdMapper;
import com.isums.assetservice.infrastructures.repositories.IotThresholdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IotThresholdService {

    private final IotThresholdRepository thresholdRepository;
    private final DynamoDbClient dynamoDb;
    private final IotThresholdMapper iotThresholdMapper;

    @Value("${app.ddb.thresholdsTable}")
    private String thresholdsTable;

    public void seedDefaults(UUID houseId) {
        List<IotThreshold> defaults = List.of(
                build(houseId, null, "voltage",   180.0, 260.0, "WARNING"),
                build(houseId, null, "current",   null,  20.0,  "WARNING"),
                build(houseId, null, "power",     null,  5000.0,"WARNING"),
                build(houseId, null, "frequency", 49.0,  51.0,  "WARNING"),
                build(houseId, null, "w_lpm",     null,  50.0,  "CRITICAL"),
                build(houseId, null, "d_w_tot",   null,  100.0, "WARNING"),
                build(houseId, null, "gas_ppm",   null,  300.0, "CRITICAL")
        );

        defaults.stream()
                .filter(t -> thresholdRepository
                        .findByHouseIdAndAreaIdIsNullAndMetric(t.getHouseId(), t.getMetric()).isEmpty())
                .forEach(t -> {
                    thresholdRepository.save(t);
                    syncToDynamoDB(t);
                });
    }

    private IotThreshold build(UUID houseId, UUID areaId, String metric,
                               Double min, Double max, String severity) {
        return IotThreshold.builder()
                .houseId(houseId).areaId(areaId).metric(metric)
                .minVal(min).maxVal(max)
                .severity(Severity.valueOf(severity))
                .build();
    }

    public List<ThresholdResponse> getAllByHouse(UUID houseId) {
        List<IotThreshold> threshold = thresholdRepository.findByHouseId(houseId);
        return iotThresholdMapper.toResponseList(threshold);
    }

    public List<ThresholdResponse> getAllByArea(UUID areaId) {
        List<IotThreshold> threshold = thresholdRepository.findByAreaId(areaId);
        return iotThresholdMapper.toResponseList(threshold);
    }

    public ThresholdResponse upsertHouseLevel(
            UUID houseId, String metric, ThresholdRequest req) {
        IotThreshold t = thresholdRepository.findByHouseIdAndAreaIdIsNullAndMetric(houseId, metric)
                .orElse(IotThreshold.builder()
                        .houseId(houseId)
                        .metric(metric)
                        .build());
        return saveAndSync(t, req);
    }

    private ThresholdResponse saveAndSync(IotThreshold t, ThresholdRequest req) {
        t.setMinVal(req.getMinVal());
        t.setMaxVal(req.getMaxVal());
        t.setEnabled(req.getEnabled());
        t.setSeverity(Severity.valueOf(req.getSeverity()));
        IotThreshold saved = thresholdRepository.save(t);
        try {
            syncToDynamoDB(saved);
        } catch (Exception e) {
            log.warn("[THRESHOLD] DynamoDB sync failed for {}/{}: {}",
                    saved.getHouseId(), saved.getMetric(), e.getMessage());
        }
        return iotThresholdMapper.toResponse(saved);
    }

    private void syncToDynamoDB(IotThreshold t) {
        String pk = t.getAreaId() != null
                ? "area#" + t.getAreaId()
                : "house#" + t.getHouseId();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", s(pk));
        item.put("sk", s("metric#" + t.getMetric()));
        item.put("house_id", s(t.getHouseId().toString()));
        item.put("metric", s(t.getMetric()));
        item.put("enabled", bool(t.getEnabled()));
        item.put("severity", s(t.getSeverity().name()));
        if (t.getAreaId() != null)
            item.put("area_id", s(t.getAreaId().toString()));
        if (t.getMinVal() != null)
            item.put("min_val", n(t.getMinVal().toString()));
        if (t.getMaxVal() != null)
            item.put("max_val", n(t.getMaxVal().toString()));

        dynamoDb.putItem(PutItemRequest.builder()
                .tableName(thresholdsTable)
                .item(item).build());
    }

    public ThresholdResponse upsertAreaLevel(
            UUID houseId, UUID areaId, String metric, ThresholdRequest req) {
        IotThreshold t = thresholdRepository.findByAreaIdAndMetric(areaId, metric)
                .orElse(IotThreshold.builder()
                        .houseId(houseId)
                        .areaId(areaId)
                        .metric(metric)
                        .build());
        return saveAndSync(t, req);
    }

    public void deleteHouseLevel(UUID houseId, String metric) {
        thresholdRepository.deleteByHouseIdAndAreaIdIsNullAndMetric(houseId, metric);
        deleteFromDynamoDB("house#" + houseId, metric);
    }

    public void deleteAreaLevel(UUID areaId, String metric) {
        thresholdRepository.deleteByAreaIdAndMetric(areaId, metric);
        deleteFromDynamoDB("area#" + areaId, metric);
    }

    private void deleteFromDynamoDB(String pk, String metric) {
        dynamoDb.deleteItem(DeleteItemRequest.builder().tableName(thresholdsTable)
                .key(Map.of(
                        "pk", s(pk),
                        "sk", s("metric#" + metric)
                )).build());
    }

    private AttributeValue s(String v) {
        return AttributeValue.builder().s(v).build();
    }

    private AttributeValue n(String v) {
        return AttributeValue.builder().n(v).build();
    }

    private AttributeValue bool(Boolean v) {
        return AttributeValue.builder().bool(v).build();
    }

}
