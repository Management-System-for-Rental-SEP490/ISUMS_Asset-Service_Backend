package com.isums.assetservice.domains.dtos.AssetCategoryDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssetCategoryDto {
    private UUID id;
    /** Resolved name for the request locale. */
    private String name;
    /** Full name translations — all locales (for edit forms). */
    private Map<String, String> nameTranslations;
    private int compensationPercent;
    /** Resolved description for the request locale. */
    private String description;
    /** Full description translations — all locales (for edit forms). */
    private Map<String, String> descriptionTranslations;
    private String detectionType;
}
