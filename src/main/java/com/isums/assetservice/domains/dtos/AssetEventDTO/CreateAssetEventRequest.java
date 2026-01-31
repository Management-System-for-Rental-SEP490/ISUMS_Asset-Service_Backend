package com.isums.assetservice.domains.dtos.AssetEventDTO;

import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.enums.AssetEventType;
import jakarta.persistence.*;

import java.util.UUID;

public record CreateAssetEventRequest(
         String description,
         UUID createBy,
         UUID assetId
) {

}
