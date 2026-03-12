package com.isums.assetservice.domains.dtos;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ThresholdRequest {
    private Double minVal;
    private Double maxVal;
    @Builder.Default
    private Boolean enabled = true;
    @Builder.Default
    private String severity = "WARNING";
}
