package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.entities.AssetTagLog;

import java.util.List;
import java.util.UUID;

public interface AssetTagLogService
{
    List<AssetTagLog> getLogsByTag(String tagValue);
    List<AssetTagLog> getLogsByAsset(UUID assetId);
}
