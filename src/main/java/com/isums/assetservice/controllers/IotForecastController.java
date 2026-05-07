package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.*;
import com.isums.assetservice.infrastructures.abstracts.IotForecastService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/assets/houses/{houseId}/iot/forecast")
@RequiredArgsConstructor
public class IotForecastController {

    private final IotForecastService forecastService;
    private final MessageSource messageSource;

    private String msg(String code) {
        try {
            return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return code;
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT','LANDLORD','MANAGER')")
    public ApiResponse<ForecastAllDto> getForecastAll(@PathVariable UUID houseId, @RequestParam(required = false) String month) {
        ForecastAllDto result = forecastService.getForecastAll(houseId, month);
        return ApiResponses.ok(result, msg("iot.forecast"));
    }

    @GetMapping("/{metric}")
    @PreAuthorize("hasAnyRole('TENANT','LANDLORD','MANAGER')")
    public ApiResponse<ForecastScopeDto> getForecast(@PathVariable UUID houseId, @PathVariable String metric,
                                                     @RequestParam(required = false) UUID areaId,
                                                     @RequestParam(required = false) String month) {

        if (!metric.equals("electricity") && !metric.equals("water")) {
            throw new IllegalArgumentException("metric must be 'electricity' or 'water'");
        }

        ForecastScopeDto result = forecastService.getForecast(houseId, areaId, metric, month);
        return ApiResponses.ok(result, msg("iot.forecast"));
    }

    @PostMapping("/trigger")
    @PreAuthorize("hasAnyRole('LANDLORD','MANAGER')")
    public ApiResponse<Void> triggerForecast(@PathVariable UUID houseId) {
        forecastService.triggerForecast(houseId);
        return ApiResponses.ok(null, msg("iot.forecast_triggered"));
    }
}
