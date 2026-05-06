package com.isums.assetservice.domains.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.isums.assetservice.domains.enums.UtilityMetric;
import com.isums.assetservice.domains.enums.UtilityStatus;

import java.time.Instant;

/**
 * Event emitted by asset-service when a (house, utility) tile
 * transitions into a worse status band on the alerts dashboard —
 * e.g. {@code GOOD → WARNING} or {@code WARNING → CRITICAL}.
 *
 * <p>Consumed by notification-service to deliver push + email alerts
 * to the landlord/manager. We fire only on state <em>transitions</em>,
 * not every check — the scheduler keeps the previous-state vector in
 * Redis so that a house stuck at 85% doesn't hammer the landlord's
 * inbox every hour.
 *
 * <p>Topic: {@code utility.consumption.alert}. Key: {@code houseId}
 * so all events for a given house land on the same partition and
 * preserve ordering. Value: JSON serialisation of this record.
 *
 * <p>Schema evolution: add fields at the end; keep existing fields
 * nullable-compatible. Consumers should ignore unknown fields. If a
 * breaking change is unavoidable, bump the topic name to
 * {@code utility.consumption.alert.v2} rather than try to version
 * the payload.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UtilityThresholdExceededEvent(
        String eventId,           // UUID v4 — client-side dedup key
        String houseId,
        String houseName,         // snapshot for notification convenience
        String landlordUserId,    // legacy recipient field; kept for backwards-compatible consumers
        String tenantUserId,      // current renter/tenant internal users.id for SMS/voice dispatch
        UtilityMetric metric,
        UtilityStatus previousStatus,
        UtilityStatus currentStatus,
        Double currentUsage,
        Double monthlyLimit,
        Double usagePercent,
        String unit,
        String month,             // yyyy-MM
        Long occurredAt           // epoch millis, VN tz
) {
    public static UtilityThresholdExceededEvent now(
            String eventId,
            String houseId,
            String houseName,
            String landlordUserId,
            String tenantUserId,
            UtilityMetric metric,
            UtilityStatus previous,
            UtilityStatus current,
            Double currentUsage,
            Double monthlyLimit,
            Double usagePercent,
            String month
    ) {
        return new UtilityThresholdExceededEvent(
                eventId,
                houseId,
                houseName,
                landlordUserId,
                tenantUserId,
                metric,
                previous,
                current,
                currentUsage,
                monthlyLimit,
                usagePercent,
                metric.unit(),
                month,
                Instant.now().toEpochMilli()
        );
    }
}
