package com.isums.assetservice.domains.dtos.AssetEventDTO;

import com.isums.assetservice.domains.enums.AssetEventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssetEventDto {
    private UUID id;
    private AssetEventType eventType;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createBy;
    private UUID assetId;
}
