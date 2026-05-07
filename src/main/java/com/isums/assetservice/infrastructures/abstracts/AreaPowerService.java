package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.dtos.AreaPowerStateResponse;
import com.isums.assetservice.domains.enums.PowerAction;

import java.util.UUID;

public interface AreaPowerService {

    AreaPowerStateResponse toggleAreaPower(UUID houseId, UUID areaId, PowerAction action, UUID requesterId);

    AreaPowerStateResponse getAreaPowerState(UUID houseId, UUID areaId);
}