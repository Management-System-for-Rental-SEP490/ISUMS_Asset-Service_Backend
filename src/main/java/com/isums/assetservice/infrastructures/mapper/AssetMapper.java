package com.isums.assetservice.infrastructures.mapper;

import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.dtos.AssetImageDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.dtos.AssetTagDto.AssetTagDto;
import com.isums.assetservice.domains.entities.*;
import com.isums.assetservice.domains.enums.TagType;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface AssetMapper {
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(target = "tags",ignore = true)
    @Mapping(target = "images",ignore = true)
    AssetItemDto mapAssetItem(AssetItem assetItem);
    List<AssetItemDto> mapAssetItems(List<AssetItem> assetItems);

    AssetCategoryDto mapAssetCategory(AssetCategory assetCategory);
    List<AssetCategoryDto> mapAssetCategories(List<AssetCategory> assetCategories);

    AssetImageDto mapAssetImage(AssetImage assetImage);
    List<AssetImageDto> maAssetImages(Collection<AssetImage> assetImages);

    @Mapping(source = "assetItem.id", target = "assetId")
    @Mapping(source = "assetItem.displayName",target = "assetName")
    @Mapping(target = "images", ignore = true)
    AssetEventDto mapAssetEvent(AssetEvent assetEvent);
    List<AssetEventDto> maAssetEvents(Collection<AssetEvent> assetEvents);

    @Mapping(source = "assetItem.houseId", target = "houseId")
    @Mapping(source = "assetItem.id", target = "assetId")
    AssetTagDto tagDto(AssetTag assetTag);
    List<AssetTagDto> tagDtos(List<AssetTag> assetTagDtos);
}