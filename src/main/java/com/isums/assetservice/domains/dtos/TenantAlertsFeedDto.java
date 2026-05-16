package com.isums.assetservice.domains.dtos;

import lombok.Builder;

import java.util.List;

@Builder
public record TenantAlertsFeedDto(
        List<TenantAlertDto> items,
        long totalCount,
        long criticalCount,
        long warningCount,
        long infoCount,
        long lifeSafetyCount,
        long acknowledgedCount,
        long pendingCount
) {}
