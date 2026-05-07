package com.isums.assetservice.domains.enums;

import java.util.Locale;
import java.util.Optional;

/**
 * The set of utility types surfaced on the landlord/manager alerts
 * dashboard. Each enum value pins:
 *
 * <ul>
 *   <li>{@link #forecastKey} — the value written to DynamoDB
 *       {@code esp32_forecast.metric} by the EIF Prophet pipeline
 *       ({@code Z:\EIF\h2o_training\prophet_forecast.py}). Has to
 *       match exactly or the forecast lookup misses.</li>
 *   <li>{@link #thresholdKey} — the value written to
 *       {@code iot_thresholds.metric} that represents a <em>monthly
 *       consumption cap</em> for this utility. The per-sensor
 *       thresholds already in the table (voltage, current, power_W,
 *       w_lpm, …) are for instantaneous readings and deliberately
 *       kept separate; this new row per (house, utility) carries the
 *       monthly kWh / m³ ceiling the landlord agreed to with the
 *       tenant in the lease contract.</li>
 *   <li>{@link #unit} — the SI unit for the monthly total. Never
 *       translated — kWh / m³ are international.</li>
 * </ul>
 *
 * <p><b>Gas intentionally not included.</b> The ISUMS pipeline doesn't
 * ingest gas consumption today, per product decision (2026-04). If it
 * lands later, add a third enum value; the service / controller / FE
 * all read from this enum so no other change is needed beyond a new
 * locale string.
 */
public enum UtilityMetric {
    ELECTRICITY("electricity", "electricity_monthly_kwh", "kWh"),
    WATER     ("water",       "water_monthly_m3",       "m³");

    private final String forecastKey;
    private final String thresholdKey;
    private final String unit;

    UtilityMetric(String forecastKey, String thresholdKey, String unit) {
        this.forecastKey = forecastKey;
        this.thresholdKey = thresholdKey;
        this.unit = unit;
    }

    public String forecastKey() { return forecastKey; }
    public String thresholdKey() { return thresholdKey; }
    public String unit()         { return unit; }

    /**
     * Accepts either the lowercased forecast key ("electricity")
     * or the enum name ("ELECTRICITY"). Returns empty for unknown
     * values so the controller can 400 instead of 500.
     */
    public static Optional<UtilityMetric> fromRequestParam(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String key = raw.trim().toLowerCase(Locale.ROOT);
        for (UtilityMetric m : values()) {
            if (m.forecastKey.equals(key) || m.name().toLowerCase(Locale.ROOT).equals(key)) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }
}
