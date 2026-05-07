package com.isums.assetservice.domains.dtos;

import com.isums.assetservice.domains.enums.PowerAction;
import jakarta.validation.constraints.NotNull;

public record AreaPowerToggleRequest(
        @NotNull(message = "action is required (ON or OFF)")
        PowerAction action
) {}