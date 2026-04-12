package com.isums.assetservice.domains.dtos;

import com.isums.assetservice.domains.enums.NodeCapability;

import java.util.Set;
import java.util.UUID;

public record ProvisionNodeRequest(
        String token,
        String serial,
        UUID areaId,
        Set<NodeCapability> capabilities
) {}
