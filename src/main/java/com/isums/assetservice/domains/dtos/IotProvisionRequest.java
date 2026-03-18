package com.isums.assetservice.domains.dtos;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record IotProvisionRequest(
        @NotBlank String deviceId,
        UUID areaId
) {
}
