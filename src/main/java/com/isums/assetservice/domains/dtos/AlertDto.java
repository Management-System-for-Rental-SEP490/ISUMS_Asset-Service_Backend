package com.isums.assetservice.domains.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AlertDto(
        String alertId,
        String houseId,
        String areaId,
        String areaName,
        String thing,
        String alertType,
        String title,
        String detail,
        String metric,
        Double value,
        String level,
        Boolean resolved,
        Long ts,
        String date
) {}