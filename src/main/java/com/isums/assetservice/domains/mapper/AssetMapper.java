package com.isums.assetservice.domains.mapper;

import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.dtos.AssetImageDTO.AssetImageDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.domains.entities.AssetImage;
import com.isums.assetservice.domains.entities.AssetItem;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AssetMapper {
    AssetItemDto mapAssetItem(AssetItem assetItem);
    List<AssetItemDto> mapAssetItems(List<AssetItem> assetItems);

    AssetCategoryDto mapAssetCategory(AssetCategory assetCategory);
    List<AssetCategoryDto> mapAssetCategories(List<AssetCategory> assetCategories);

    AssetImageDto mapAssetImage(AssetImage assetImage);
    List<AssetImageDto> maAssetImages(List<AssetImage> assetImages);
}