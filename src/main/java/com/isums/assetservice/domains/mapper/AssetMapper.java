package com.isums.assetservice.domains.mapper;

import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.dtos.AssetImageDTO.AssetImageDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.domains.entities.AssetEvent;
import com.isums.assetservice.domains.entities.AssetImage;
import com.isums.assetservice.domains.entities.AssetItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AssetMapper {
    @Mapping(source = "category.id", target = "categoryId")
    AssetItemDto mapAssetItem(AssetItem assetItem);
    List<AssetItemDto> mapAssetItems(List<AssetItem> assetItems);

    AssetCategoryDto mapAssetCategory(AssetCategory assetCategory);
    List<AssetCategoryDto> mapAssetCategories(List<AssetCategory> assetCategories);

    @Mapping(source = "assetItem.id", target = "assetId")
    AssetImageDto mapAssetImage(AssetImage assetImage);
    List<AssetImageDto> maAssetImages(List<AssetImage> assetImages);

    @Mapping(source = "assetItem.id", target = "assetId")
    AssetEventDto mapAssetEvent(AssetEvent assetEvent);
    List<AssetEventDto> maAssetEvents(List<AssetEvent> assetEvents);
}