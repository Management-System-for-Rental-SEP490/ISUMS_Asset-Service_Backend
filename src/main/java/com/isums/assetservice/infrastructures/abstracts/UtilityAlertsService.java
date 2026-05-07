package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.dtos.UtilityAlertsResponse;
import com.isums.assetservice.domains.enums.UtilityMetric;

/**
 * Read-side service for the landlord/manager utility alerts dashboard.
 *
 * <p>Responsibility: given the caller (by Keycloak id) and a utility
 * metric, return one {@link UtilityAlertsResponse} envelope scoped to
 * the houses that user is authorised to see — LANDLORD sees theirs,
 * MANAGER sees everything in their region(s), TECH_STAFF sees their
 * N:M assignments. TENANT is handled by a different endpoint because
 * they need a single-house detail view rather than a fleet roll-up.
 *
 * <p>The service composes three data sources:
 * <ol>
 *   <li>{@code esp32_forecast} (DynamoDB) — usedSoFar + totalEstimate
 *       + daysLeft, populated by the EIF Prophet pipeline. Served via
 *       the existing {@link IotForecastService}.</li>
 *   <li>{@code iot_thresholds} (Postgres) — monthly consumption cap
 *       (landlord-configured in the lease contract).</li>
 *   <li>{@code esp32_alerts} (DynamoDB) — per-sensor instantaneous
 *       alerts, sampled for "active unresolved today" count.</li>
 * </ol>
 *
 * <p>No write side in Phase 1 (the dashboard is read-only). Phase 2
 * adds a scheduled crossover detector that emits Kafka events when a
 * tile transitions GOOD → WARNING → CRITICAL — see
 * {@code ThresholdCrossoverScheduler}.
 */
public interface UtilityAlertsService {

    /**
     * Build the alerts envelope for the caller.
     *
     * @param keycloakId {@code jwt.getSubject()} — used to look up the
     *                   internal user id + roles via user-service.
     * @param metric     which utility tab the FE is asking for.
     * @return the envelope, possibly with empty items if the caller
     *         has no houses in scope (UI shows "no houses" empty
     *         state rather than erroring).
     */
    UtilityAlertsResponse listAlerts(String keycloakId, UtilityMetric metric);
}
