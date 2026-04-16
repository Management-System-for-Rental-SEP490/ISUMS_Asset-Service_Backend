package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.dtos.ForecastAllDto;
import com.isums.assetservice.domains.dtos.ForecastScopeDto;
import com.isums.assetservice.domains.dtos.ForecastTriggerResultDto;

import java.util.UUID;

public interface IotForecastService {

    ForecastAllDto getForecastAll(UUID houseId, String month);

    ForecastScopeDto getForecast(UUID houseId, UUID areaId, String metric, String month);

    void triggerForecast(UUID houseId);

    ForecastTriggerResultDto triggerAllForecasts();
}
