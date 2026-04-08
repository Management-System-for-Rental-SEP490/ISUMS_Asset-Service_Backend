package com.isums.assetservice.domains.dtos;

public record OtaStatusResponse(
        String jobId,
        String status,
        Integer progress,
        String error,
        long updatedAt
) {}
