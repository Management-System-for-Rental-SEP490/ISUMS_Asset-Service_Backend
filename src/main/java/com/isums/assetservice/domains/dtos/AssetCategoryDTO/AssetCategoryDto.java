package com.isums.assetservice.domains.dtos.AssetCategoryDTO;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssetCategoryDto {
    private UUID id;
    private String name;
    private int compensationPercent;
    private String description;
}
