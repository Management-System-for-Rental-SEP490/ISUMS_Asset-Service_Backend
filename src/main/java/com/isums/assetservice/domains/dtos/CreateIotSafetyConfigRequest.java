package com.isums.assetservice.domains.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.databind.JsonNode;

public record CreateIotSafetyConfigRequest(
        @NotBlank @Size(max = 80) String version,
        JsonNode configJson,
        @Size(max = 1000) String notes
) {}
