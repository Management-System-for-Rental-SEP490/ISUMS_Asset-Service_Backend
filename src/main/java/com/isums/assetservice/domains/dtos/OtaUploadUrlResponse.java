package com.isums.assetservice.domains.dtos;

public record OtaUploadUrlResponse(
        String uploadUrl, String downloadUrl, int expiresIn
) {}
