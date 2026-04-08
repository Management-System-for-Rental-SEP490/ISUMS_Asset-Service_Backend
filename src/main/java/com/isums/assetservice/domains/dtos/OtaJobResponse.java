package com.isums.assetservice.domains.dtos;

public record OtaJobResponse(
        String jobId, String thingName, String target,
        String serial, String version, String url, long createdAt
) {
}
