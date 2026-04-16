package com.isums.assetservice.domains.dtos.AssetItemDTO;

import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.dtos.AssetImageDto;
import com.isums.assetservice.domains.dtos.AssetTagDto.AssetTagDto;
import com.isums.assetservice.domains.enums.AssetStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetItemDto {
    private UUID id;
    private UUID functionAreaId;
    private UUID houseId;
    private UUID categoryId;
    /** Resolved display name for the request locale (use in list/detail views). */
    private String displayName;
    /** Full translation map — all locales (use in edit forms). */
    private Map<String, String> translations;
    private String serialNumber;
    private int conditionPercent;
    private AssetStatus status;
    private String note;
    private Instant updateAt;
    private List<AssetTagDto> tags;
    private List<AssetImageDto> images;
}
