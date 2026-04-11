package com.isums.assetservice.domains.dtos;

import java.util.UUID;

public record AppAccessChangedEvent(
        UUID contractId,
        UUID houseId,
        UUID tenantId,
        boolean restricted,
        String reason,
        String messageId
) {}