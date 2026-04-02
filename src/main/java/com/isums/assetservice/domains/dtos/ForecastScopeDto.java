package com.isums.assetservice.domains.dtos;

import java.util.List;
import java.util.UUID;

public record ForecastScopeDto(
        String metric,
        String unit,
        String scope,
        UUID areaId,
        String areaName,
        double usedSoFar,
        double forecastRemaining,
        double totalEstimate,
        double confidenceLower,
        double confidenceUpper,
        int daysLeft,
        String trend,
        int trainingRows,
        List<ForecastDailyPoint> dailyForecast,
        long forecastedAt
) {}
