package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.ForecastAllDto;
import com.isums.assetservice.domains.dtos.ForecastDailyPoint;
import com.isums.assetservice.domains.dtos.ForecastScopeDto;
import com.isums.assetservice.infrastructures.abstracts.IotForecastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IotForecastServiceImpl implements IotForecastService {

    private final DynamoDbClient dynamoDb;
    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;

    @Value("${app.ddb.forecastTable:esp32_forecast}")
    private String forecastTable;

    @Value("${app.lambda.forecastDispatcher:esp32-forecast-dispatcher}")
    private String forecastDispatcherArn;

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    @Cacheable(value = "iotForecast", key = "#houseId + ':' + #month")
    public ForecastAllDto getForecastAll(UUID houseId, String month) {
        if (month == null || month.isBlank()) {
            month = LocalDate.now(VN).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        ForecastAllDto.ForecastMetricDto electricity = buildMetricDto(houseId, month, "electricity");
        ForecastAllDto.ForecastMetricDto water = buildMetricDto(houseId, month, "water");

        return new ForecastAllDto(houseId, month, electricity, water);
    }

    @Override
    public ForecastScopeDto getForecast(UUID houseId, UUID areaId, String metric, String month) {
        if (month == null || month.isBlank()) {
            month = LocalDate.now(VN).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        String pk = areaId != null
                ? houseId + "#" + areaId + "#" + metric
                : houseId + "#" + metric;

        return getFromDynamoDB(pk, month);
    }

    @Override
    public void triggerForecast(UUID houseId) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of("houseId", houseId.toString()));
            lambdaClient.invoke(InvokeRequest.builder()
                    .functionName(forecastDispatcherArn)
                    .invocationType(InvocationType.EVENT)
                    .payload(SdkBytes.fromString(payload, StandardCharsets.UTF_8))
                    .build());
            log.info("[Forecast] Triggered for houseId={}", houseId);
        } catch (Exception e) {
            log.error("[Forecast] Trigger failed houseId={}: {}", houseId, e.getMessage(), e);
        }
    }

    private ForecastAllDto.ForecastMetricDto buildMetricDto(UUID houseId, String month, String metric) {

        String housePk = houseId + "#" + metric;
        ForecastScopeDto house = getFromDynamoDB(housePk, month);

        Map<String, ForecastScopeDto> areas = new LinkedHashMap<>();
        try {
            QueryResponse r = dynamoDb.query(QueryRequest.builder()
                    .tableName(forecastTable)
                    .indexName("house-month-index")
                    .keyConditionExpression("houseId = :hid AND #m = :month")
                    .expressionAttributeNames(Map.of("#m", "month"))
                    .expressionAttributeValues(Map.of(
                            ":hid", av(houseId.toString()),
                            ":month", av(month)
                    ))
                    .build());

            for (Map<String, AttributeValue> item : r.items()) {
                String scope = str(item, "scope");
                String itemMetric = str(item, "metric");
                String areaId = str(item, "areaId");

                if ("area".equals(scope) && metric.equals(itemMetric) && areaId != null) {
                    ForecastScopeDto dto = mapToDto(item);
                    if (dto != null) areas.put(areaId, dto);
                }
            }
        } catch (Exception e) {
            log.warn("[Forecast] GSI query failed metric={}: {}", metric, e.getMessage());
        }

        return new ForecastAllDto.ForecastMetricDto(house, areas.isEmpty() ? null : areas);
    }

    private ForecastScopeDto getFromDynamoDB(String pk, String month) {
        try {
            GetItemResponse r = dynamoDb.getItem(GetItemRequest.builder()
                    .tableName(forecastTable)
                    .key(Map.of(
                            "pk", av(pk),
                            "month", av(month)
                    ))
                    .build());

            if (!r.hasItem()) return null;
            return mapToDto(r.item());
        } catch (Exception e) {
            log.warn("[Forecast] getItem failed pk={}: {}", pk, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private ForecastScopeDto mapToDto(Map<String, AttributeValue> item) {
        try {
            String areaIdStr = str(item, "areaId");

            List<ForecastDailyPoint> daily = new ArrayList<>();
            AttributeValue dailyAttr = item.get("dailyForecast");
            if (dailyAttr != null && dailyAttr.hasL()) {
                for (AttributeValue entry : dailyAttr.l()) {
                    if (entry.hasM()) {
                        Map<String, AttributeValue> m = entry.m();
                        daily.add(new ForecastDailyPoint(
                                str(m, "ds"),
                                num(m, "yhat"),
                                num(m, "lower"),
                                num(m, "upper")
                        ));
                    }
                }
            }

            return new ForecastScopeDto(
                    str(item, "metric"),
                    str(item, "unit"),
                    str(item, "scope"),
                    areaIdStr != null ? UUID.fromString(areaIdStr) : null,
                    str(item, "areaName"),
                    num(item, "usedSoFar"),
                    num(item, "forecastRemaining"),
                    num(item, "totalEstimate"),
                    num(item, "confidenceLower"),
                    num(item, "confidenceUpper"),
                    (int) numLong(item, "daysLeft"),
                    str(item, "trend"),
                    (int) numLong(item, "trainingRows"),
                    daily,
                    numLong(item, "forecastedAt")
            );
        } catch (Exception e) {
            log.warn("[Forecast] mapToDto failed: {}", e.getMessage());
            return null;
        }
    }

    private AttributeValue av(String v) {
        return AttributeValue.builder().s(v).build();
    }

    private String str(Map<String, AttributeValue> m, String key) {
        AttributeValue v = m.get(key);
        return (v != null && v.s() != null) ? v.s() : null;
    }

    private double num(Map<String, AttributeValue> m, String key) {
        AttributeValue v = m.get(key);
        if (v == null) return 0.0;
        if (v.n() != null) return Double.parseDouble(v.n());
        return 0.0;
    }

    private long numLong(Map<String, AttributeValue> m, String key) {
        AttributeValue v = m.get(key);
        if (v == null) return 0L;
        if (v.n() != null) return Long.parseLong(v.n());
        return 0L;
    }
}
