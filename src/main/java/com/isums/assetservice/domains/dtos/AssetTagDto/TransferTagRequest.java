package com.isums.assetservice.domains.dtos.AssetTagDto;

import java.util.UUID;

public record TransferTagRequest(
        String tagValue,
        UUID newAssetId
) {
}
