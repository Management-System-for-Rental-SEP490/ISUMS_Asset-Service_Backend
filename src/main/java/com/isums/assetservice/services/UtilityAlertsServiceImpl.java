package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.ForecastScopeDto;
import com.isums.assetservice.domains.dtos.UtilityAlertItem;
import com.isums.assetservice.domains.dtos.UtilityAlertSummary;
import com.isums.assetservice.domains.dtos.UtilityAlertsResponse;
import com.isums.assetservice.domains.entities.IotThreshold;
import com.isums.assetservice.domains.enums.UtilityMetric;
import com.isums.assetservice.domains.enums.UtilityStatus;
import com.isums.assetservice.infrastructures.abstracts.IotForecastService;
import com.isums.assetservice.infrastructures.abstracts.UtilityAlertsService;
import com.isums.assetservice.infrastructures.grpcs.GrpcUserClient;
import com.isums.assetservice.infrastructures.grpcs.HouseGrpcImpl;
import com.isums.assetservice.infrastructures.repositories.IotThresholdRepository;
import com.isums.houseservice.grpc.HouseResponse;
import com.isums.userservice.grpc.UserResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UtilityAlertsServiceImpl implements UtilityAlertsService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final double WARN_RATIO = 0.80;
    private static final double CRIT_RATIO = 1.00;

    private final IotForecastService iotForecastService;
    private final IotThresholdRepository thresholdRepository;
    private final HouseGrpcImpl houseGrpc;
    private final GrpcUserClient userGrpc;
    private final DynamoDbClient dynamoDb;
    private final MeterRegistry meterRegistry;

    @Value("${app.ddb.alertsTable:esp32_alerts}")
    private String alertsTable;

    @Override
    @Cacheable(
            value = "utilityAlerts",
            key = "#keycloakId + ':' + #metric.name() + ':' + T(java.time.LocalDate).now(T(java.time.ZoneId).of('Asia/Ho_Chi_Minh')).format(T(java.time.format.DateTimeFormatter).ofPattern('yyyy-MM'))"
    )
    public UtilityAlertsResponse listAlerts(String keycloakId, UtilityMetric metric) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return doListAlerts(keycloakId, metric);
        } finally {
            sample.stop(meterRegistry.timer(
                    "utility_alerts_build_duration",
                    "metric", metric.name().toLowerCase(Locale.ROOT)));
        }
    }

    private UtilityAlertsResponse doListAlerts(String keycloakId, UtilityMetric metric) {
        String month = LocalDate.now(VN).format(MONTH_FMT);
        String today = LocalDate.now(VN).format(DATE_FMT);

        UserResponse caller;
        try {
            caller = userGrpc.getUserIdAndRoleByKeyCloakId(keycloakId);
        } catch (Exception e) {
            log.warn("[UtilityAlerts] user lookup failed keycloakId={}: {}", keycloakId, e.getMessage());
            return emptyResponse(metric, month);
        }

        UUID userId;
        try {
            userId = UUID.fromString(caller.getId());
        } catch (Exception e) {
            log.warn("[UtilityAlerts] caller has non-UUID id={}, returning empty", caller.getId());
            return emptyResponse(metric, month);
        }

        Set<String> roles = new java.util.HashSet<>(caller.getRolesList());

        List<HouseResponse> houses = resolveScopedHouses(userId, roles);
        if (houses.isEmpty()) {
            log.debug("[UtilityAlerts] no houses in scope userId={} roles={}", userId, roles);
            return emptyResponse(metric, month);
        }

        Map<UUID, Double> limitByHouse = loadMonthlyLimits(houses, metric);

        List<UtilityAlertItem> items = new ArrayList<>(houses.size());
        double sumUsage = 0;
        int countWithData = 0;
        AtomicInteger housesOverThreshold = new AtomicInteger(0);

        for (HouseResponse h : houses) {
            UUID houseId;
            try { houseId = UUID.fromString(h.getId()); } catch (Exception skip) { continue; }

            ForecastScopeDto fc = safeForecast(houseId, metric, month);

            Double currentUsage   = fc != null ? fc.usedSoFar()     : null;
            Double forecastTotal  = fc != null ? fc.totalEstimate() : null;
            Integer daysLeft      = fc != null ? fc.daysLeft()      : null;
            Long lastReadingAt    = fc != null && fc.forecastedAt() > 0 ? fc.forecastedAt() : null;

            Double monthlyLimit = limitByHouse.get(houseId);
            Double usagePercent = (monthlyLimit != null && monthlyLimit > 0 && currentUsage != null)
                    ? 100.0 * currentUsage / monthlyLimit
                    : null;

            UtilityStatus status = deriveStatus(currentUsage, monthlyLimit);
            if (status == UtilityStatus.WARNING || status == UtilityStatus.CRITICAL) {
                housesOverThreshold.incrementAndGet();
            }

            AlertCountToday ac = countActiveAlerts(houseId, today);

            items.add(new UtilityAlertItem(
                    houseId,
                    h.getName(),
                    null ,
                    currentUsage,
                    forecastTotal,
                    monthlyLimit,
                    usagePercent == null ? null : round(usagePercent, 1),
                    metric.unit(),
                    status,
                    ac.count,
                    daysLeft,
                    lastReadingAt,
                    ac.lastTs
            ));

            if (currentUsage != null) {
                sumUsage += currentUsage;
                countWithData++;
            }
        }

        UtilityAlertSummary summary = new UtilityAlertSummary(
                countWithData > 0 ? round(sumUsage, 2)                   : null,
                countWithData > 0 ? round(sumUsage / countWithData, 2)   : null,
                houses.size(),
                housesOverThreshold.get()
        );

        meterRegistry.counter(
                "utility_alerts_requests_total",
                "metric", metric.name().toLowerCase(Locale.ROOT),
                "houses", String.valueOf(houses.size())
        ).increment();

        return new UtilityAlertsResponse(metric, metric.unit(), month, items, summary);
    }

    private List<HouseResponse> resolveScopedHouses(UUID userId, Set<String> roles) {
        if (roles.contains("LANDLORD") || roles.contains("ADMIN")) {
            return houseGrpc.listHousesByLandlord(userId);
        }
        if (roles.contains("MANAGER")) {
            return houseGrpc.listHousesByManager(userId);
        }
        return Collections.emptyList();
    }

    private Map<UUID, Double> loadMonthlyLimits(List<HouseResponse> houses, UtilityMetric metric) {
        List<UUID> ids = houses.stream()
                .map(h -> { try { return UUID.fromString(h.getId()); } catch (Exception e) { return null; } })
                .filter(java.util.Objects::nonNull)
                .toList();
        if (ids.isEmpty()) return Collections.emptyMap();

        Map<UUID, Double> out = new HashMap<>();
        for (UUID id : ids) {
            thresholdRepository
                    .findByHouseIdAndAreaIdIsNullAndMetric(id, metric.thresholdKey())
                    .filter(t -> Boolean.TRUE.equals(t.getEnabled()))
                    .map(IotThreshold::getMaxVal)
                    .filter(v -> v != null && v > 0)
                    .ifPresent(v -> out.put(id, v));
        }
        return out;
    }

    private ForecastScopeDto safeForecast(UUID houseId, UtilityMetric metric, String month) {
        try {
            return iotForecastService.getForecast(houseId, null, metric.forecastKey(), month);
        } catch (Exception e) {
            log.warn("[UtilityAlerts] forecast fetch failed houseId={} metric={}: {}",
                    houseId, metric.forecastKey(), e.getMessage());
            return null;
        }
    }

    private AlertCountToday countActiveAlerts(UUID houseId, String date) {
        try {
            String pk = houseId + "#" + date;
            QueryResponse r = dynamoDb.query(QueryRequest.builder()
                    .tableName(alertsTable)
                    .indexName("house-date-ts-index")
                    .keyConditionExpression("houseDatePartition = :pk")
                    .filterExpression("attribute_not_exists(resolved) OR resolved = :f")
                    .expressionAttributeValues(Map.of(
                            ":pk", AttributeValue.builder().s(pk).build(),
                            ":f",  AttributeValue.builder().bool(false).build()
                    ))
                    .limit(100)
                    .scanIndexForward(false)
                    .build());

            int count = r.items().size();
            Long lastTs = null;
            if (!r.items().isEmpty()) {
                AttributeValue ts = r.items().get(0).get("ts");
                if (ts != null && ts.n() != null) {
                    try { lastTs = Long.parseLong(ts.n()); } catch (NumberFormatException ignore) { }
                }
            }
            return new AlertCountToday(count, lastTs);
        } catch (Exception e) {
            log.warn("[UtilityAlerts] alert count failed houseId={}: {}", houseId, e.getMessage());
            return new AlertCountToday(0, null);
        }
    }

    private UtilityStatus deriveStatus(Double currentUsage, Double monthlyLimit) {
        if (currentUsage == null || monthlyLimit == null || monthlyLimit <= 0) {
            return UtilityStatus.NO_DATA;
        }
        double ratio = currentUsage / monthlyLimit;
        if (ratio >= CRIT_RATIO) return UtilityStatus.CRITICAL;
        if (ratio >= WARN_RATIO) return UtilityStatus.WARNING;
        return UtilityStatus.GOOD;
    }

    private UtilityAlertsResponse emptyResponse(UtilityMetric metric, String month) {
        return new UtilityAlertsResponse(
                metric,
                metric.unit(),
                month,
                Collections.emptyList(),
                new UtilityAlertSummary(null, null, 0, 0)
        );
    }

    private static double round(double v, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(v * factor) / factor;
    }

    private record AlertCountToday(int count, Long lastTs) { }
}

