package com.isums.assetservice.domains.dtos.AssetEventDTO;

import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.enums.AssetEventType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

public class AssetEventDto {
    private UUID id;
    private AssetEventType eventType;
    private String description;
    private Instant createdAt;
    private UUID createBy;
    private UUID assetId;
}
