package com.isums.assetservice.domains.dtos.AssetImageDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetImageDto {
    private UUID id;
    private UUID assetId;
    private String imageUrl;
    private String note;
    private Instant createdAt;
}
