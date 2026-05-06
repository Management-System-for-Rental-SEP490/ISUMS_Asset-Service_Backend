package com.isums.assetservice.schedulers;

import com.isums.assetservice.domains.entities.IotController;
import com.isums.assetservice.domains.entities.IotThreshold;
import com.isums.assetservice.domains.enums.UtilityMetric;
import com.isums.assetservice.domains.enums.UtilityStatus;
import com.isums.assetservice.domains.events.UtilityThresholdExceededEvent;
import com.isums.assetservice.infrastructures.abstracts.IotForecastService;
import com.isums.assetservice.infrastructures.grpcs.HouseGrpcImpl;
import com.isums.assetservice.infrastructures.repositories.IotControllerRepository;
import com.isums.assetservice.infrastructures.repositories.IotThresholdRepository;
import com.isums.houseservice.grpc.HouseResponse;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UtilityThresholdCrossoverScheduler {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final Duration STATE_TTL = Duration.ofHours(48);

    private static final double WARN_RATIO = 0.80;
    private static final double CRIT_RATIO = 1.00;

    private final IotControllerRepository controllerRepository;
    private final IotThresholdRepository thresholdRepository;
    private final IotForecastService iotForecastService;
    private final HouseGrpcImpl houseGrpc;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${app.kafka.topic.utility-alert:utility.consumption.alert}")
    private String topic;

    @Value("${app.utility-alerts.scheduler.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "0 7 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void scan() {
        if (!enabled) {
            log.debug("[UtilityAlerts] scheduler disabled by flag, skipping");
            return;
        }
        long started = System.currentTimeMillis();
        int emitted = 0;
        int checked = 0;

        String month = LocalDate.now(VN).format(MONTH_FMT);
        List<IotController> controllers = controllerRepository.findAll();

        for (IotController ctrl : controllers) {
            UUID houseId = ctrl.getHouseId();
            for (UtilityMetric metric : UtilityMetric.values()) {
                try {
                    checked++;
                    emitted += checkAndMaybeEmit(houseId, metric, month) ? 1 : 0;
                } catch (Exception e) {

                    log.warn("[UtilityAlerts] scan failed houseId={} metric={}: {}",
                            houseId, metric, e.getMessage());
                }
            }
        }

        long elapsed = System.currentTimeMillis() - started;
        log.info("[UtilityAlerts] sweep done controllers={} checked={} emitted={} elapsedMs={}",
                controllers.size(), checked, emitted, elapsed);
        meterRegistry.counter("utility_alerts_scheduler_sweeps_total").increment();
        meterRegistry.counter("utility_alerts_scheduler_events_total").increment(emitted);
    }

    private boolean checkAndMaybeEmit(UUID houseId, UtilityMetric metric, String month) {
        Double limit = thresholdRepository
                .findByHouseIdAndAreaIdIsNullAndMetric(houseId, metric.thresholdKey())
                .filter(t -> Boolean.TRUE.equals(t.getEnabled()))
                .map(IotThreshold::getMaxVal)
                .filter(v -> v != null && v > 0)
                .orElse(null);
        if (limit == null) return false;

        var fc = iotForecastService.getForecast(houseId, null, metric.forecastKey(), month);
        if (fc == null) return false;

        double used = fc.usedSoFar();
        UtilityStatus current = deriveStatus(used, limit);
        if (current == UtilityStatus.GOOD || current == UtilityStatus.NO_DATA) {

            persistState(houseId, metric, current);
            return false;
        }

        UtilityStatus previous = loadState(houseId, metric);
        if (previous == current) {

            return false;
        }
        if (previous == UtilityStatus.CRITICAL && current == UtilityStatus.WARNING) {

            persistState(houseId, metric, current);
            return false;
        }

        HouseResponse houseMeta = safeHouseMeta(houseId);
        String tenantUserId = houseMeta != null ? houseMeta.getUserRentalId() : null;
        // Legacy field name kept so older notification-service builds can still
        // consume the event. house.user_rental_id is the current renter/tenant.
        String landlordUserId = tenantUserId;
        String houseName = houseMeta != null ? houseMeta.getName() : houseId.toString();

        UtilityThresholdExceededEvent event = UtilityThresholdExceededEvent.now(
                UUID.randomUUID().toString(),
                houseId.toString(),
                houseName,
                landlordUserId,
                tenantUserId,
                metric,
                previous == null ? UtilityStatus.GOOD : previous,
                current,
                round(used, 2),
                round(limit, 2),
                round(100.0 * used / limit, 1),
                month
        );

        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, houseId.toString(), json);
            persistState(houseId, metric, current);
            log.info("[UtilityAlerts] emitted houseId={} metric={} {}→{} usage={}%",
                    houseId, metric, previous, current, round(100.0 * used / limit, 1));
            return true;
        } catch (Exception e) {
            log.error("[UtilityAlerts] kafka emit failed houseId={} metric={}: {}",
                    houseId, metric, e.getMessage(), e);

            return false;
        }
    }

    private HouseResponse safeHouseMeta(UUID houseId) {
        try { return houseGrpc.getHouseById(houseId); }
        catch (Exception e) {
            log.warn("[UtilityAlerts] houseGrpc.getHouseById failed houseId={}: {}", houseId, e.getMessage());
            return null;
        }
    }

    private UtilityStatus deriveStatus(double used, double limit) {
        if (limit <= 0) return UtilityStatus.NO_DATA;
        double r = used / limit;
        if (r >= CRIT_RATIO) return UtilityStatus.CRITICAL;
        if (r >= WARN_RATIO) return UtilityStatus.WARNING;
        return UtilityStatus.GOOD;
    }

    private String stateKey(UUID houseId, UtilityMetric metric) {
        return "utility-alert-state:" + houseId + ":" + metric.name();
    }

    private UtilityStatus loadState(UUID houseId, UtilityMetric metric) {
        try {
            String v = redis.opsForValue().get(stateKey(houseId, metric));
            if (v == null) return null;
            return UtilityStatus.valueOf(v);
        } catch (Exception e) {
            log.warn("[UtilityAlerts] redis read failed: {}", e.getMessage());
            return null;
        }
    }

    private void persistState(UUID houseId, UtilityMetric metric, UtilityStatus status) {
        try {
            redis.opsForValue().set(stateKey(houseId, metric), status.name(), STATE_TTL);
        } catch (Exception e) {
            log.warn("[UtilityAlerts] redis write failed: {}", e.getMessage());
        }
    }

    private static double round(double v, int scale) {
        double f = Math.pow(10, scale);
        return Math.round(v * f) / f;
    }
}
