package com.isums.assetservice.domains.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ThresholdResponse {
    private UUID id;
    private UUID houseId;
    private UUID areaId;
    private String metric;
    private Double minVal;
    private Double maxVal;
    private Boolean enabled;
    private String severity;
    private LocalDateTime updatedAt;
}