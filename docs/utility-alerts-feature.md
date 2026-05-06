# Utility Alerts — production feature handoff

## What shipped

Replaces the mock `/utilities` dashboard with a real fleet-wide alerts
surface driven by the EIF forecast pipeline + `iot_thresholds`. Landlord
and manager see every house they're responsible for, with the ones
crossing their monthly electricity/water cap surfaced first — and get
an email when a house crosses the threshold.

## End-to-end flow

```
ESP32 → MQTT → AWS IoT → Lambda → esp32_raw (DDB)
                                    │
                                    ▼
                 EIF Xeon job (daily) → esp32_usage_agg + esp32_forecast
                                    │
          ┌─────────────────────────┼─────────────────────────┐
          ▼                         ▼                         ▼
     READ PATH                EMIT PATH                  ALERT PATH
  UtilityAlertsService   UtilityThresholdCrossover    UtilityAlertEventListener
  (on landlord hit)      Scheduler (hourly)            (notification-service)
          │                         │                         │
          ▼                         ▼                         ▼
  Redis cache 5min         Kafka utility.consumption.alert   email vi/en/ja
          │                         │
          ▼                         ▼
  FE Utilities.jsx          Redis state store (48h TTL, debounce)
```

## Files added

### asset-service (BE)
| File | Purpose |
|---|---|
| `domains/enums/UtilityMetric.java` | ELECTRICITY / WATER enum + forecast/threshold/unit keys |
| `domains/enums/UtilityStatus.java` | GOOD / WARNING / CRITICAL / NO_DATA |
| `domains/dtos/UtilityAlertItem.java` | One tile |
| `domains/dtos/UtilityAlertSummary.java` | Top-of-dashboard KPIs |
| `domains/dtos/UtilityAlertsResponse.java` | Envelope |
| `domains/events/UtilityThresholdExceededEvent.java` | Kafka payload |
| `infrastructures/abstracts/UtilityAlertsService.java` | Interface |
| `services/UtilityAlertsServiceImpl.java` | Read-side orchestrator + @Cacheable |
| `controllers/UtilityAlertsController.java` | `GET /api/assets/utility-alerts` |
| `schedulers/UtilityThresholdCrossoverScheduler.java` | Hourly cron + Redis debounce + Kafka emit |
| `infrastructures/grpcs/HouseGrpcImpl.java` | **extended** with listHousesByLandlord/Manager |

### asset-service config + docs
| File | Change |
|---|---|
| `src/main/resources/application.properties` | `app.ddb.usageAggTable`, `app.ddb.forecastTable`, `app.cache.utility-alerts.ttl-seconds`, `app.kafka.topic.utility-alert`, `app.utility-alerts.scheduler.enabled` |
| `src/main/resources/messages*.properties` | `utility_alerts.retrieved` key in vi/en/ja |
| `docs/iam-policy-utility-alerts.md` | IAM read-only policy for `esp32_usage_agg` + `esp32_forecast` |

### notification-service (BE)
| File | Purpose |
|---|---|
| `domains/events/UtilityThresholdExceededEvent.java` | Mirror of asset-service event |
| `infrastructures/listeners/UtilityAlertEventListener.java` | Kafka consumer + idempotency + email dispatch |
| `infrastructures/seeders/UtilityAlertTemplateSeeder.java` | Seeds `utility_threshold_exceeded` email templates vi/en/ja |

### web-application (FE)
| File | Purpose |
|---|---|
| `features/utilities/api/utilities.api.js` | REST client (getUtilityAlerts, getHouseAlerts, upsertMonthlyLimit) |
| `features/utilities/hooks/useUtilityAlerts.js` | Module-scope cached hook, 60s TTL + manual refresh |
| `features/utilities/components/HouseAlertDrawer.jsx` | Per-house drawer with alerts + action strip |
| `app/layout/Utilities.jsx` | **rewritten** — real fetch, skeleton/empty/error, tiles, summary KPIs, drawer |
| `lib/api-endpoints.js` | **extended** with `UTILITY_ENDPOINTS` |
| `locales/{vi,en,ja}/common.json` | `utilitiesPage.*` extended — gas removed, drawer/status/states keys added |

### tests
| File | Coverage |
|---|---|
| `services/UtilityAlertsServiceImplTest.java` | Role scoping (LANDLORD / MANAGER / TECH_STAFF), status bands (GOOD / WARNING / CRITICAL), NO_DATA degradation, DDB alert-count happy + failure paths |

## Request / response

```
GET /api/assets/utility-alerts?metric=electricity
Authorization: Bearer <JWT>

200 OK
{
  "data": {
    "metric": "ELECTRICITY",
    "unit": "kWh",
    "month": "2026-04",
    "items": [
      {
        "houseId": "…",
        "houseName": "Nhà Q1 #12",
        "currentUsage": 850.0,
        "forecastTotal": 1020.0,
        "monthlyLimit": 1000.0,
        "usagePercent": 85.0,
        "unit": "kWh",
        "status": "WARNING",
        "activeAlertCount": 2,
        "daysLeft": 8,
        "lastAlertAt": 1714020930000
      }
    ],
    "summary": {
      "totalUsage": 3490.0,
      "avgPerHouse": 872.5,
      "houseCount": 4,
      "housesOverThreshold": 2
    }
  },
  "message": "Lấy danh sách cảnh báo tiện ích thành công"
}
```

## Metric names

New rows in `iot_thresholds` table (one per house + utility):
- `metric = 'electricity_monthly_kwh'`, `max_val = <kWh cap>`
- `metric = 'water_monthly_m3'`, `max_val = <m³ cap>`
- `area_id = NULL` (house-level)

Existing per-sensor thresholds (`voltage`, `current`, `power_W`, etc.) are
unaffected — they continue to power the `esp32_alerts` stream which the
drawer renders, but they're not what gates the tile status.

## Kafka event

**Topic**: `utility.consumption.alert`
**Key**: `houseId` (preserves ordering per house)
**Value** (JSON):
```json
{
  "eventId": "uuid-v4",
  "houseId": "…",
  "houseName": "Nhà Q1 #12",
  "landlordUserId": "…",
  "metric": "ELECTRICITY",
  "previousStatus": "GOOD",
  "currentStatus": "WARNING",
  "currentUsage": 850.0,
  "monthlyLimit": 1000.0,
  "usagePercent": 85.0,
  "unit": "kWh",
  "month": "2026-04",
  "occurredAt": 1714020930000
}
```

Consumer: notification-service → email to landlord (template
`utility_threshold_exceeded`, default locale vi_VN).

## Rollout checklist

1. Merge branch.
2. **AWS**: attach IAM policy from `docs/iam-policy-utility-alerts.md` to
   the asset-service ECS task role (or the dev IAM user).
3. **Kafka**: ensure topic `utility.consumption.alert` exists with RF=1
   (dev) or RF=3 (prod). Auto-created on first send if broker has
   `auto.create.topics.enable=true`; otherwise create manually via
   Kafka UI on `:9091`.
4. Restart asset-service and notification-service (IDE Run Dashboard).
5. Hard-refresh FE browser.
6. Seed a threshold via
   `PUT /api/assets/houses/{id}/iot/thresholds/electricity_monthly_kwh`
   with body `{"maxVal": 1000, "enabled": true, "severity": "WARNING"}`.
7. Wait ~5 minutes for Redis cache cold path; load `/utilities` in the
   landlord session — tile should show real data.
8. For push notification smoke test: set `maxVal` very low (e.g. 10 kWh)
   on a house that already has forecast data, wait for the `:07` past
   hour cron (or call the scheduler method manually via an admin
   endpoint in a future hardening pass), check the landlord's inbox
   and Kafka UI for the event.

## Observability

- Micrometer counters: `utility_alerts_requests_total{metric}`,
  `utility_alerts_scheduler_sweeps_total`,
  `utility_alerts_scheduler_events_total`.
- Micrometer timer: `utility_alerts_build_duration{metric}`.
- Logs: all lines prefixed `[UtilityAlerts]` or `[UtilityAlert]` in
  asset-service and notification-service respectively.

## Known deferred items

| Item | Why deferred | Ticket |
|---|---|---|
| `preferredLanguage` on UserResponse (proto) → landlord locale for email | Needs cross-team proto change | next sprint |
| Integration test with Testcontainers + LocalStack DDB | Requires LocalStack image pull + test infra; unit covers the orchestration logic | next sprint |
| Flyway baseline migration for `iot_thresholds` (replace `ddl-auto=update`) | Requires downtime coordination | hardening pass |
| Drawer 30-day chart | Needs a `esp32_usage_agg` → daily-series endpoint | next sprint |
| Region name enrichment (`regionName` on item) | Requires extra house-service gRPC call per row | next sprint |
| Gas metric | Not ingested today | when hardware lands |
