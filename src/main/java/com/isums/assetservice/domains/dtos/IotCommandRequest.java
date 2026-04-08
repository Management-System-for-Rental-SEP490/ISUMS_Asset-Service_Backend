package com.isums.assetservice.domains.dtos;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

public record IotCommandRequest(
        @NotBlank String cmd,
        @Nullable String serial
) {}