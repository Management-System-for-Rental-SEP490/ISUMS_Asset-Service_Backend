package com.isums.assetservice.domains.dtos;

import java.util.List;
import java.util.UUID;

public record BatchUpdateAssetRequest(
        UUID jobId,
        List<AssetUpdateItem> updates

) {
    public record AssetUpdateItem(
            UUID assetId,
            Integer conditionPercent,
            String note,
            List<String> assetImages
    ) {}
}
