package com.isums.assetservice.domains.dtos.AssetImageDTO;

import java.time.Instant;
import java.util.UUID;

public record CreateAssetImageRequest(
         UUID assetId,
         String imageUrl,
         String note
) {
}
