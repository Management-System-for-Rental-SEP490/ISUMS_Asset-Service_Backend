package com.isums.assetservice.domains.dtos;

import com.isums.assetservice.domains.enums.NodeCapability;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateCapabilitiesRequest(
        @NotNull
        @Size(min = 1, message = "At least one capability required")
        Set<NodeCapability> capabilities
) {}