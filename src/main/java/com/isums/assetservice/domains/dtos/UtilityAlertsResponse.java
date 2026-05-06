package com.isums.assetservice.domains.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.isums.assetservice.domains.enums.UtilityMetric;

import java.util.List;

/**
 * Envelope for {@code GET /api/assets/utility-alerts}.
 *
 * <p>One payload per (metric, month) pair — the FE issues one fetch
 * per tab (Electricity / Water) and caches per-tab via React Query.
 *
 * <p>{@code metric} and {@code unit} are echoed back so the FE can
 * render the unit suffix ("kWh" / "m³") without hard-coding and the
 * React Query cache can use them as part of the key without risking
 * stale data if the request param was coerced / normalised server-
 * side.
 *
 * <p>{@code month} is {@code YYYY-MM} — the billing month, always
 * computed in VN time in the service so the dashboard matches what
 * the tenant sees on their invoice.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UtilityAlertsResponse(
        UtilityMetric metric,
        String unit,
        String month,
        List<UtilityAlertItem> items,
        UtilityAlertSummary summary
) { }
