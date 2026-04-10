package com.isums.assetservice.domains.dtos;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

public record OtaRequest(
        @NotBlank String target,
        @NotBlank String url,
        @NotBlank String version,
        @Nullable String serial,
        @Nullable String md5
) {}
