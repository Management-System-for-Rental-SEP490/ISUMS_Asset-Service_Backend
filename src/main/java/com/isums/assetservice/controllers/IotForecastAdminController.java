package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.ForecastTriggerResultDto;
import com.isums.assetservice.infrastructures.abstracts.IotForecastService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assets/iot/forecast")
@RequiredArgsConstructor
public class IotForecastAdminController {

    private final IotForecastService forecastService;
    private final MessageSource messageSource;

    private String msg(String code) {
        try {
            return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return code;
        }
    }

    @PostMapping("/trigger-all")
    @PreAuthorize("hasAnyRole('LANDLORD','MANAGER')")
    public ApiResponse<ForecastTriggerResultDto> triggerAllForecasts() {
        ForecastTriggerResultDto result = forecastService.triggerAllForecasts();
        return ApiResponses.ok(result, msg("iot.forecast_triggered_all"));
    }
}
