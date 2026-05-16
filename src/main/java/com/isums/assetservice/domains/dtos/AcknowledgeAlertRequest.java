package com.isums.assetservice.domains.dtos;

import jakarta.validation.constraints.Size;

public record AcknowledgeAlertRequest(
        @Size(max = 500) String note
) {}
