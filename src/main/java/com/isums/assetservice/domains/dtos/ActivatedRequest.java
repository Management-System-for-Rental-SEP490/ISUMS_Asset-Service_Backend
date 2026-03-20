package com.isums.assetservice.domains.dtos;

import jakarta.validation.constraints.NotBlank;

public record ActivatedRequest(@NotBlank String thingName) {
}
