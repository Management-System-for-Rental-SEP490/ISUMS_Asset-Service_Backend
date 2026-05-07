package com.isums.assetservice.domains.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.isums.assetservice.domains.enums.UtilityStatus;

import java.util.UUID;

/**
 * One row on the alerts dashboard — a single (house, utility) tile.
 *
 * <p>Shape chosen so the FE can render everything without a second
 * round-trip: usage progress bar, trend chip, threshold text, drill-
 * down badge, and the deep-link to the per-house detail page.
 *
 * <ul>
 *   <li>{@code currentUsage} — kWh / m³ consumed month-to-date,
 *       sourced from {@code esp32_forecast.usedSoFar}.</li>
 *   <li>{@code forecastTotal} — model-estimated end-of-month total
 *       ({@code esp32_forecast.totalEstimate}). Displayed as a
 *       secondary line: "forecast 10 250 kWh by 30 Apr".</li>
 *   <li>{@code monthlyLimit} — landlord-agreed cap from
 *       {@code iot_thresholds.max_val} where {@code metric} is the
 *       utility's {@code thresholdKey}. Null if no cap configured —
 *       UI then shows "Set limit →" CTA.</li>
 *   <li>{@code usagePercent} — {@code 100 · currentUsage / monthlyLimit}.
 *       Null when {@code monthlyLimit} is null.</li>
 *   <li>{@code status} — derived, see {@link UtilityStatus}.</li>
 *   <li>{@code activeAlertCount} — unresolved rows in
 *       {@code esp32_alerts} for this house today — not the monthly
 *       overage, but per-sensor instantaneous alerts (e.g. over-
 *       voltage spike, water leak flow). Landlord wants both signals
 *       on the same tile.</li>
 *   <li>{@code daysLeft} — days remaining in the current billing
 *       month, from the forecast. Helps the landlord judge urgency.</li>
 * </ul>
 *
 * <p>Fields that can't be populated (no forecast row, no threshold,
 * no alerts today) come back as {@code null} and the serialiser drops
 * them via {@code NON_NULL}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UtilityAlertItem(
        UUID houseId,
        String houseName,
        String regionName,

        Double currentUsage,
        Double forecastTotal,
        Double monthlyLimit,
        Double usagePercent,

        String unit,
        UtilityStatus status,

        Integer activeAlertCount,
        Integer daysLeft,

        Long lastReadingAt,
        Long lastAlertAt
) { }
