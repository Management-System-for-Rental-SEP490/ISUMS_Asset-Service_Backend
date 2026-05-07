package com.isums.assetservice.domains.dtos;

import java.util.List;

public record IotSafetyConfigDto(
        List<IotSafetySensorDto> activeSensors,
        List<IotSafetyCapabilityGapDto> capabilityGaps,
        List<IotSafetyThresholdDto> thresholds,
        List<IotSafetyScoreComponentDto> scoreComponents,
        List<IotSafetyBandDto> bands,
        String disclaimer,
        String scoreFormulaDescription,
        String standardsApplied,
        String version,
        String effectiveFrom,
        String notes
) {}
