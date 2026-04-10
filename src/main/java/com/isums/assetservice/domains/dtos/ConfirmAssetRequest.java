package com.isums.assetservice.domains.dtos;

import com.isums.assetservice.domains.enums.AssetStatus;

public record ConfirmAssetRequest(
        AssetStatus status
) {
}
