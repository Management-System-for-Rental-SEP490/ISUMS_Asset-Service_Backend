package com.isums.assetservice.domains.dtos.AssetEventDTO;

import com.isums.assetservice.domains.dtos.AssetImageDto;
import com.isums.assetservice.domains.enums.AssetEventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssetEventDto {
    private UUID id;
    private UUID jobId;
    private AssetEventType eventType;
    private Integer previousCondition;
    private Integer currentCondition;
    private String note;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID assetId;
    private String assetName;
    private List<AssetImageDto> images;
}
