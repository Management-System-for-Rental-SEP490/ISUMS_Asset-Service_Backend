package com.isums.assetservice.domains.dtos.AssetEventDTO;

import com.isums.assetservice.domains.enums.AssetEventType;

public record UpdateAssetEventRequest (
    AssetEventType status
){}
