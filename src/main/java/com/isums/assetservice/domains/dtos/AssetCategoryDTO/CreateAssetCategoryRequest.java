package com.isums.assetservice.domains.dtos.AssetCategoryDTO;

import jakarta.persistence.Column;

import java.util.UUID;

public record CreateAssetCategoryRequest (
     String name,
     int compensationPercent,
     String description
){
    }
