package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.ForecastScopeDto;
import com.isums.assetservice.domains.dtos.UtilityAlertsResponse;
import com.isums.assetservice.domains.entities.IotThreshold;
import com.isums.assetservice.domains.enums.UtilityMetric;
import com.isums.assetservice.domains.enums.UtilityStatus;
import com.isums.assetservice.infrastructures.abstracts.IotForecastService;
import com.isums.assetservice.infrastructures.grpcs.GrpcUserClient;
import com.isums.assetservice.infrastructures.grpcs.HouseGrpcImpl;
import com.isums.assetservice.infrastructures.repositories.IotThresholdRepository;
import com.isums.houseservice.grpc.HouseResponse;
import com.isums.userservice.grpc.UserResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Focused unit tests for UtilityAlertsServiceImpl — the orchestrator
 * that the /api/assets/utility-alerts endpoint depends on. We exercise
 * the branches that matter to the landlord experience:
 *
 * <ul>
 *   <li>Role-based scope filtering (LANDLORD → listHousesByLandlord,
 *       MANAGER → listHousesByManager, unknown role → empty).</li>
 *   <li>Status derivation against the 80% / 100% band split.</li>
 *   <li>Graceful degradation when forecast or threshold is missing.</li>
 * </ul>
 *
 * <p>Integration coverage for the DynamoDB / Postgres / gRPC joins
 * belongs in a separate {@code @SpringBootTest} with LocalStack +
 * Testcontainers — this test stays fast and schema-free.
 */
@ExtendWith(MockitoExtension.class)
// We share a set of default stubs from setUp() across the nested groups
// (userGrpc, houseGrpc, dynamoDb empty-result). Some tests — TECH_STAFF
// and the two alert-count tests — deliberately bypass those paths or
// override the DDB stub, which trips Mockito's strict-stubbing check.
// Lenient is safer here than picking apart which stub each test uses;
// the trade-off is that an accidentally-unused stub won't fail loud,
// but the assertions are already strong enough to catch wiring bugs.
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UtilityAlertsServiceImpl")
class UtilityAlertsServiceImplTest {

    @Mock private IotForecastService iotForecastService;
    @Mock private IotThresholdRepository thresholdRepository;
    @Mock private HouseGrpcImpl houseGrpc;
    @Mock private GrpcUserClient userGrpc;
    @Mock private DynamoDbClient dynamoDb;

    private UtilityAlertsServiceImpl service;
    private MeterRegistry meterRegistry;

    private static final String KEYCLOAK_ID = "kc-sub-abc";
    private UUID userId;
    private UUID houseId;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new UtilityAlertsServiceImpl(
                iotForecastService,
                thresholdRepository,
                houseGrpc,
                userGrpc,
                dynamoDb,
                meterRegistry
        );
        userId = UUID.randomUUID();
        houseId = UUID.randomUUID();

        // Zero-alerts DDB response used by most tests — specific tests
        // can override via a lenient when() in their own setup.
        when(dynamoDb.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(Collections.emptyList()).build());
    }

    @Nested
    @DisplayName("scope resolution")
    class Scope {

        @Test
        @DisplayName("TECH_STAFF-only caller → empty items, summary with zero houses")
        void techStaffGetsEmpty() {
            stubCaller(List.of("TECH_STAFF"));

            UtilityAlertsResponse res = service.listAlerts(KEYCLOAK_ID, UtilityMetric.ELECTRICITY);

            assertThat(res.items()).isEmpty();
            assertThat(res.summary().houseCount()).isZero();
            assertThat(res.summary().housesOverThreshold()).isZero();
        }

        @Test
        @DisplayName("LANDLORD → houses from listHousesByLandlord")
        void landlordFetchedFromLandlordRoute() {
            stubCaller(List.of("LANDLORD"));
            when(houseGrpc.listHousesByLandlord(eq(userId)))
                    .thenReturn(List.of(house(houseId, "Nhà Q1")));
            // No threshold / no forecast → NO_DATA
            when(thresholdRepository.findByHouseIdAndAreaIdIsNullAndMetric(any(), any()))
                    .thenReturn(Optional.empty());

            UtilityAlertsResponse res = service.listAlerts(KEYCLOAK_ID, UtilityMetric.ELECTRICITY);

            assertThat(res.items()).hasSize(1);
            assertThat(res.items().get(0).status()).isEqualTo(UtilityStatus.NO_DATA);
        }

        @Test
        @DisplayName("MANAGER → houses from listHousesByManager")
        void managerFetchedFromManagerRoute() {
            stubCaller(List.of("MANAGER"));
            when(houseGrpc.listHousesByManager(eq(userId)))
                    .thenReturn(List.of(house(houseId, "Nhà Q3")));
            when(thresholdRepository.findByHouseIdAndAreaIdIsNullAndMetric(any(), any()))
                    .thenReturn(Optional.empty());

            UtilityAlertsResponse res = service.listAlerts(KEYCLOAK_ID, UtilityMetric.WATER);

            assertThat(res.items()).hasSize(1);
            assertThat(res.metric()).isEqualTo(UtilityMetric.WATER);
            assertThat(res.unit()).isEqualTo("m³");
        }
    }

    @Nested
    @DisplayName("status derivation")
    class Status {

        @Test
        @DisplayName("usage < 80% of limit → GOOD")
        void good() {
            prepareHouseWith(500.0 /*used*/, 1000.0 /*limit*/);
            UtilityAlertsResponse res = service.listAlerts(KEYCLOAK_ID, UtilityMetric.ELECTRICITY);
            assertThat(res.items().get(0).status()).isEqualTo(UtilityStatus.GOOD);
            assertThat(res.summary().housesOverThreshold()).isZero();
        }

        @Test
        @DisplayName("usage 80–99% → WARNING")
        void warning() {
            prepareHouseWith(850.0, 1000.0);
            UtilityAlertsResponse res = service.listAlerts(KEYCLOAK_ID, UtilityMetric.ELECTRICITY);
            assertThat(res.items().get(0).status()).isEqualTo(UtilityStatus.WARNING);
            assertThat(res.summary().housesOverThreshold()).isEqualTo(1);
        }

        @Test
        @DisplayName("usage ≥ 100% → CRITICAL")
        void critical() {
            prepareHouseWith(1050.0, 1000.0);
            UtilityAlertsResponse res = service.listAlerts(KEYCLOAK_ID, UtilityMetric.ELECTRICITY);
            assertThat(res.items().get(0).status()).isEqualTo(UtilityStatus.CRITICAL);
            assertThat(res.summary().housesOverThreshold()).isEqualTo(1);
        }

        @Test
        @DisplayName("threshold disabled → NO_DATA (don't alert)")
        void disabledThresholdSkipped() {
            stubCaller(List.of("LANDLORD"));
            when(houseGrpc.listHousesByLandlord(eq(userId)))
                    .thenReturn(List.of(house(houseId, "Nhà Q5")));
            when(thresholdRepository.findByHouseIdAndAreaIdIsNullAndMetric(eq(houseId), any()))
                    .thenReturn(Optional.of(threshold(1000.0, false))); // disabled
            when(iotForecastService.getForecast(eq(houseId), any(), any(), any()))
                    .thenReturn(forecast(1500.0));

            UtilityAlertsResponse res = service.listAlerts(KEYCLOAK_ID, UtilityMetric.ELECTRICITY);

            assertThat(res.items().get(0).status()).isEqualTo(UtilityStatus.NO_DATA);
            assertThat(res.items().get(0).monthlyLimit()).isNull();
        }

        @Test
        @DisplayName("forecast missing → NO_DATA even with threshold")
        void forecastMissing() {
            stubCaller(List.of("LANDLORD"));
            when(houseGrpc.listHousesByLandlord(eq(userId)))
                    .thenReturn(List.of(house(houseId, "Nhà Q7")));
            when(thresholdRepository.findByHouseIdAndAreaIdIsNullAndMetric(eq(houseId), any()))
                    .thenReturn(Optional.of(threshold(1000.0, true)));
            when(iotForecastService.getForecast(eq(houseId), any(), any(), any()))
                    .thenReturn(null);

            UtilityAlertsResponse res = service.listAlerts(KEYCLOAK_ID, UtilityMetric.ELECTRICITY);

            assertThat(res.items().get(0).status()).isEqualTo(UtilityStatus.NO_DATA);
            assertThat(res.items().get(0).currentUsage()).isNull();
        }
    }

    @Nested
    @DisplayName("alert count from DynamoDB")
    class AlertCount {

        @Test
        @DisplayName("DDB query returns rows → count echoed in tile")
        void countEchoed() {
            prepareHouseWith(500.0, 1000.0);
            // Override the default zero-items stub from setUp() with 3
            // unresolved alerts for today's partition.
            when(dynamoDb.query(any(QueryRequest.class)))
                    .thenReturn(QueryResponse.builder()
                            .items(List.of(
                                    java.util.Map.of("ts", AttributeValue.builder().n("1234567890").build()),
                                    java.util.Map.of("ts", AttributeValue.builder().n("1234567891").build()),
                                    java.util.Map.of("ts", AttributeValue.builder().n("1234567892").build())
                            ))
                            .build());

            UtilityAlertsResponse res = service.listAlerts(KEYCLOAK_ID, UtilityMetric.ELECTRICITY);

            assertThat(res.items().get(0).activeAlertCount()).isEqualTo(3);
            assertThat(res.items().get(0).lastAlertAt()).isEqualTo(1234567890L);
        }

        @Test
        @DisplayName("DDB failure → count falls back to 0, tile still rendered")
        void ddbFailureDegrades() {
            prepareHouseWith(500.0, 1000.0);
            when(dynamoDb.query(any(QueryRequest.class)))
                    .thenThrow(new RuntimeException("boom"));

            UtilityAlertsResponse res = service.listAlerts(KEYCLOAK_ID, UtilityMetric.ELECTRICITY);

            assertThat(res.items()).hasSize(1);
            assertThat(res.items().get(0).activeAlertCount()).isZero();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private void stubCaller(List<String> roles) {
        UserResponse user = UserResponse.newBuilder()
                .setId(userId.toString())
                .addAllRoles(roles)
                .build();
        when(userGrpc.getUserIdAndRoleByKeyCloakId(KEYCLOAK_ID)).thenReturn(user);
    }

    private void prepareHouseWith(double used, double limit) {
        stubCaller(List.of("LANDLORD"));
        when(houseGrpc.listHousesByLandlord(eq(userId)))
                .thenReturn(List.of(house(houseId, "Test House")));
        when(thresholdRepository.findByHouseIdAndAreaIdIsNullAndMetric(eq(houseId), any()))
                .thenReturn(Optional.of(threshold(limit, true)));
        when(iotForecastService.getForecast(eq(houseId), any(), any(), any()))
                .thenReturn(forecast(used));
    }

    private static HouseResponse house(UUID id, String name) {
        return HouseResponse.newBuilder()
                .setId(id.toString())
                .setName(name)
                .build();
    }

    private static IotThreshold threshold(double maxVal, boolean enabled) {
        return IotThreshold.builder()
                .id(UUID.randomUUID())
                .maxVal(maxVal)
                .enabled(enabled)
                .metric("electricity_monthly_kwh")
                .build();
    }

    private static ForecastScopeDto forecast(double usedSoFar) {
        return new ForecastScopeDto(
                "electricity", "kWh", "house",
                null, null,
                usedSoFar,
                0, usedSoFar + 100,
                0, 0,
                10, "stable", 30,
                Collections.emptyList(),
                0L, "OK", null, "prophet_monthly"
        );
    }
}
