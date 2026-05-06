package com.isums.assetservice.domains.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Aggregate row at the top of the alerts dashboard — three KPIs the
 * landlord/manager looks at first before scanning individual tiles.
 *
 * <ul>
 *   <li>{@code totalUsage} — sum of {@code currentUsage} across all
 *       houses in scope. Null if no house has data yet.</li>
 *   <li>{@code avgPerHouse} — {@code totalUsage / houseCount}. Gives
 *       a baseline to eyeball which tiles are outliers.</li>
 *   <li>{@code housesOverThreshold} — count of tiles whose status is
 *       {@code WARNING} or {@code CRITICAL}. Renders as the big red
 *       number in the summary card; the landlord's first question is
 *       always "how many houses need attention right now?".</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UtilityAlertSummary(
        Double totalUsage,
        Double avgPerHouse,
        int houseCount,
        int housesOverThreshold
) { }
