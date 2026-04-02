package com.isums.assetservice.domains.dtos;

public record ForecastDailyPoint(
        String ds,
        double yhat,
        double lower,
        double upper
) {
}
