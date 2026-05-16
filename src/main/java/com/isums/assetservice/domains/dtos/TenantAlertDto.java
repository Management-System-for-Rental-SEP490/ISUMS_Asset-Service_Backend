package com.isums.assetservice.domains.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TenantAlertDto(
        String alertId,
        String houseId,
        String houseName,
        String houseAddress,
        String areaId,
        String areaName,
        String thing,
        String alertType,
        String metric,
        String title,
        String detail,
        Double value,
        String severity,
        boolean lifeSafety,
        boolean acknowledged,
        String acknowledgedBy,
        Long acknowledgedAt,
        String resolutionNote,
        Long ts,
        String date
) {}
