package com.isums.assetservice.domains.dtos;

import java.util.List;

public record IotSafetySensorDto(
        String code,
        String displayName,
        String model,
        List<String> measures,
        String accuracyNotes,
        String calibrationStatus,
        String datasheet
) {}
