package com.isums.assetservice.domains.dtos;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateCapabilitiesRequest(
        @NotNull Set<String> capabilities
) {}