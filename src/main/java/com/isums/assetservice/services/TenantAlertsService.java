package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AcknowledgeAlertRequest;
import com.isums.assetservice.domains.dtos.TenantAlertDto;
import com.isums.assetservice.domains.dtos.TenantAlertsFeedDto;
import com.isums.assetservice.infrastructures.grpcs.GrpcUserClient;
import com.isums.assetservice.infrastructures.grpcs.HouseGrpcImpl;
import com.isums.houseservice.grpc.HouseResponse;
import com.isums.userservice.grpc.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantAlertsService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int DEFAULT_DAYS_BACK = 7;
    private static final int MAX_PER_HOUSE = 100;

    private final DynamoDbClient dynamoDb;
    private final HouseGrpcImpl houseGrpc;
    private final GrpcUserClient userGrpc;

    @Value("${app.ddb.alertsTable:esp32_alerts}")
    private String alertsTable;

    public TenantAlertsFeedDto getFeed(
            String keycloakId,
            String severityFilter,
            String statusFilter,
            Boolean lifeSafetyOnly,
            Integer daysBack,
            Integer limit) {
        UserResponse caller;
        try {
            caller = userGrpc.getUserIdAndRoleByKeyCloakId(keycloakId);
        } catch (Exception e) {
            log.warn("[TenantAlerts] user lookup failed keycloakId={}: {}", keycloakId, e.getMessage());
            return emptyFeed();
        }

        UUID userId;
        try {
            userId = UUID.fromString(caller.getId());
        } catch (Exception e) {
            return emptyFeed();
        }

        Set<String> roles = new HashSet<>(caller.getRolesList());
        List<HouseResponse> houses = resolveScopedHouses(userId, roles);
        if (houses.isEmpty()) {
            return emptyFeed();
        }

        int days = daysBack != null && daysBack > 0 && daysBack <= 30 ? daysBack : DEFAULT_DAYS_BACK;
        int hardLimit = limit != null && limit > 0 && limit <= 500 ? limit : 200;

        Map<UUID, HouseResponse> houseMap = new HashMap<>();
        List<TenantAlertDto> aggregated = new ArrayList<>();
        for (HouseResponse h : houses) {
            UUID houseId;
            try {
                houseId = UUID.fromString(h.getId());
            } catch (Exception ex) {
                continue;
            }
            houseMap.put(houseId, h);
            try {
                aggregated.addAll(queryHouse(houseId, h, days));
            } catch (Exception ex) {
                log.warn("[TenantAlerts] queryHouse failed houseId={}: {}", houseId, ex.getMessage());
            }
        }

        aggregated.sort(Comparator
                .comparingInt(TenantAlertsService::severityWeight).reversed()
                .thenComparing(Comparator.comparing(TenantAlertDto::ts, Comparator.nullsLast(Comparator.reverseOrder()))));

        List<TenantAlertDto> filtered = aggregated.stream()
                .filter(a -> matchesSeverity(a, severityFilter))
                .filter(a -> matchesStatus(a, statusFilter))
                .filter(a -> !Boolean.TRUE.equals(lifeSafetyOnly) || a.lifeSafety())
                .limit(hardLimit)
                .toList();

        long critical = filtered.stream().filter(a -> TenantAlertSeverityClassifier.CRITICAL.equals(a.severity())).count();
        long warning = filtered.stream().filter(a -> TenantAlertSeverityClassifier.WARNING.equals(a.severity())).count();
        long info = filtered.stream().filter(a -> TenantAlertSeverityClassifier.INFO.equals(a.severity())).count();
        long lifeSafety = filtered.stream().filter(TenantAlertDto::lifeSafety).count();
        long acknowledged = filtered.stream().filter(TenantAlertDto::acknowledged).count();
        long pending = filtered.size() - acknowledged;

        return TenantAlertsFeedDto.builder()
                .items(filtered)
                .totalCount(filtered.size())
                .criticalCount(critical)
                .warningCount(warning)
                .infoCount(info)
                .lifeSafetyCount(lifeSafety)
                .acknowledgedCount(acknowledged)
                .pendingCount(pending)
                .build();
    }

    public TenantAlertDto acknowledge(String keycloakId, UUID houseId, String alertId, AcknowledgeAlertRequest req) {
        if (alertId == null || alertId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "alertId required");
        }

        UserResponse caller;
        try {
            caller = userGrpc.getUserIdAndRoleByKeyCloakId(keycloakId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid actor");
        }

        Set<String> roles = new HashSet<>(caller.getRolesList());
        if (!roles.contains("LANDLORD") && !roles.contains("MANAGER") && !roles.contains("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only landlord/manager can acknowledge");
        }

        UUID actorId = UUID.fromString(caller.getId());

        if (roles.contains("MANAGER") && !roles.contains("LANDLORD") && !roles.contains("ADMIN")) {
            List<HouseResponse> managed = houseGrpc.listHousesByManager(actorId);
            boolean inScope = managed.stream().anyMatch(h -> houseId.toString().equals(h.getId()));
            if (!inScope) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
            }
        }

        long now = Instant.now().toEpochMilli();
        String note = req != null && req.note() != null && !req.note().isBlank() ? req.note().trim() : null;

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("houseId", AttributeValue.builder().s(houseId.toString()).build());
        key.put("alertId", AttributeValue.builder().s(alertId).build());

        Map<String, String> exprNames = new HashMap<>();
        exprNames.put("#r", "resolved");
        exprNames.put("#rb", "resolvedBy");
        exprNames.put("#ra", "resolvedAt");
        exprNames.put("#rn", "resolutionNote");

        Map<String, AttributeValue> exprVals = new HashMap<>();
        exprVals.put(":r", AttributeValue.builder().bool(true).build());
        exprVals.put(":rb", AttributeValue.builder().s(actorId.toString()).build());
        exprVals.put(":ra", AttributeValue.builder().n(String.valueOf(now)).build());
        if (note != null) {
            exprVals.put(":rn", AttributeValue.builder().s(note).build());
        }

        String updateExpr = note != null
                ? "SET #r = :r, #rb = :rb, #ra = :ra, #rn = :rn"
                : "SET #r = :r, #rb = :rb, #ra = :ra REMOVE #rn";

        try {
            UpdateItemResponse resp = dynamoDb.updateItem(UpdateItemRequest.builder()
                    .tableName(alertsTable)
                    .key(key)
                    .updateExpression(updateExpr)
                    .expressionAttributeNames(exprNames)
                    .expressionAttributeValues(exprVals)
                    .returnValues(ReturnValue.ALL_NEW)
                    .build());

            HouseResponse house;
            try {
                house = houseGrpc.getHouseById(houseId);
            } catch (Exception e) {
                house = null;
            }
            return mapToTenantAlertDto(resp.attributes(), house);
        } catch (Exception e) {
            log.error("[TenantAlerts] acknowledge failed houseId={} alertId={}", houseId, alertId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Acknowledge failed");
        }
    }

    private List<TenantAlertDto> queryHouse(UUID houseId, HouseResponse house, int daysBack) {
        List<TenantAlertDto> result = new ArrayList<>();
        LocalDate today = LocalDate.now(VN);
        for (int offset = 0; offset < daysBack; offset++) {
            String date = today.minusDays(offset).toString();
            String pk = houseId + "#" + date;
            QueryRequest req = QueryRequest.builder()
                    .tableName(alertsTable)
                    .indexName("house-date-ts-index")
                    .keyConditionExpression("houseDatePartition = :pk")
                    .expressionAttributeValues(Map.of(":pk", AttributeValue.builder().s(pk).build()))
                    .scanIndexForward(false)
                    .limit(MAX_PER_HOUSE)
                    .build();
            try {
                QueryResponse resp = dynamoDb.query(req);
                for (Map<String, AttributeValue> item : resp.items()) {
                    result.add(mapToTenantAlertDto(item, house));
                }
            } catch (Exception e) {
                log.warn("[TenantAlerts] DDB query failed houseId={} date={}: {}", houseId, date, e.getMessage());
            }
        }
        return result;
    }

    private TenantAlertDto mapToTenantAlertDto(Map<String, AttributeValue> item, HouseResponse house) {
        String alertType = str(item, "alertType");
        String metric = str(item, "metric");
        String level = str(item, "level");
        Double value = num(item, "value");

        TenantAlertSeverityClassifier.Classification cls =
                TenantAlertSeverityClassifier.classify(alertType, metric, level, value);

        boolean acknowledged = item.containsKey("resolved")
                && item.get("resolved").bool() != null
                && Boolean.TRUE.equals(item.get("resolved").bool());

        return TenantAlertDto.builder()
                .alertId(str(item, "alertId"))
                .houseId(str(item, "houseId"))
                .houseName(house != null ? house.getName() : null)
                .houseAddress(house != null ? house.getAddress() : null)
                .areaId(str(item, "areaId"))
                .areaName(str(item, "areaName"))
                .thing(str(item, "thing"))
                .alertType(alertType)
                .metric(metric)
                .title(str(item, "title"))
                .detail(str(item, "detail"))
                .value(value)
                .severity(cls.getSeverity())
                .lifeSafety(cls.isLifeSafety())
                .acknowledged(acknowledged)
                .acknowledgedBy(str(item, "resolvedBy"))
                .acknowledgedAt(numLong(item, "resolvedAt"))
                .resolutionNote(str(item, "resolutionNote"))
                .ts(numLong(item, "ts"))
                .date(str(item, "date"))
                .build();
    }

    private static int severityWeight(TenantAlertDto a) {
        if (a.lifeSafety() && !a.acknowledged()) return 100;
        if (TenantAlertSeverityClassifier.CRITICAL.equals(a.severity()) && !a.acknowledged()) return 80;
        if (TenantAlertSeverityClassifier.WARNING.equals(a.severity()) && !a.acknowledged()) return 60;
        if (a.acknowledged()) return 10;
        return 30;
    }

    private static boolean matchesSeverity(TenantAlertDto a, String filter) {
        if (filter == null || filter.isBlank() || "ALL".equalsIgnoreCase(filter)) return true;
        return filter.equalsIgnoreCase(a.severity());
    }

    private static boolean matchesStatus(TenantAlertDto a, String filter) {
        if (filter == null || filter.isBlank() || "ALL".equalsIgnoreCase(filter)) return true;
        if ("PENDING".equalsIgnoreCase(filter)) return !a.acknowledged();
        if ("HANDLED".equalsIgnoreCase(filter)) return a.acknowledged();
        return true;
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

    private static String str(Map<String, AttributeValue> item, String key) {
        AttributeValue v = item.get(key);
        if (v == null) return null;
        return v.s();
    }

    private static Double num(Map<String, AttributeValue> item, String key) {
        AttributeValue v = item.get(key);
        if (v == null || v.n() == null) return null;
        try {
            return Double.parseDouble(v.n());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long numLong(Map<String, AttributeValue> item, String key) {
        AttributeValue v = item.get(key);
        if (v == null || v.n() == null) return null;
        try {
            return Long.parseLong(v.n());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static TenantAlertsFeedDto emptyFeed() {
        return TenantAlertsFeedDto.builder()
                .items(Collections.emptyList())
                .totalCount(0)
                .criticalCount(0)
                .warningCount(0)
                .infoCount(0)
                .lifeSafetyCount(0)
                .acknowledgedCount(0)
                .pendingCount(0)
                .build();
    }
}
