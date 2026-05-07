package com.isums.assetservice.infrastructures.mapper;

import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.dtos.AssetImageDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.dtos.AssetTagDto.AssetTagDto;
import com.isums.assetservice.domains.entities.*;
import common.i18n.TranslationMap;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AssetMapper {

    // ── Translation helpers ───────────────────────────────────────────────────

    /** Resolve a TranslationMap to a single string using the current request locale. */
    @Named("resolve")
    default String resolve(TranslationMap tm) {
        return tm == null ? null : tm.resolve();
    }

    @Named("resolveText")
    default String resolveText(String source, TranslationMap tm) {
        if (tm == null) return source;
        String resolved = tm.resolve();
        return resolved != null && !resolved.isBlank() ? resolved : source;
    }

    /**
     * Returns translations for all locales EXCEPT the currently active one,
     * since the resolved value is already exposed as {@code displayName}/{@code name}.
     * E.g. with Accept-Language: vi → {"en":"...", "ja":"..."}
     */
    @Named("toMap")
    default Map<String, String> toMap(TranslationMap tm) {
        if (tm == null) return Map.of();
        String current = LocaleContextHolder.getLocale().getLanguage();
        return tm.getTranslations().entrySet().stream()
                .filter(e -> !e.getKey().equals(current))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // ── AssetItem ─────────────────────────────────────────────────────────────

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category", target = "category")
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(source = "displayName", target = "displayName", qualifiedByName = "resolve")
    @Mapping(target = "note", expression = "java(resolveText(assetItem.getNote(), assetItem.getNoteTranslations()))")
    @Mapping(source = "displayName", target = "translations", qualifiedByName = "toMap")
    AssetItemDto mapAssetItem(AssetItem assetItem);

    List<AssetItemDto> mapAssetItems(List<AssetItem> assetItems);

    // ── AssetCategory ─────────────────────────────────────────────────────────

    @Mapping(target = "detectionType", ignore = true)
    @Mapping(source = "name", target = "name", qualifiedByName = "resolve")
    @Mapping(source = "name", target = "nameTranslations", qualifiedByName = "toMap")
    @Mapping(source = "description", target = "description", qualifiedByName = "resolve")
    @Mapping(source = "description", target = "descriptionTranslations", qualifiedByName = "toMap")
    AssetCategoryDto mapAssetCategory(AssetCategory assetCategory);

    List<AssetCategoryDto> mapAssetCategories(List<AssetCategory> assetCategories);

    // ── AssetImage ────────────────────────────────────────────────────────────

    @Mapping(target = "url", ignore = true)
    AssetImageDto mapAssetImage(AssetImage assetImage);

    List<AssetImageDto> maAssetImages(Collection<AssetImage> assetImages);

    // ── AssetEvent ────────────────────────────────────────────────────────────

    @Mapping(source = "assetItem.id", target = "assetId")
    @Mapping(source = "assetItem.displayName", target = "assetName", qualifiedByName = "resolve")
    @Mapping(target = "oldImages", ignore = true)
    @Mapping(target = "images", ignore = true)
    AssetEventDto mapAssetEvent(AssetEvent assetEvent);

    List<AssetEventDto> maAssetEvents(Collection<AssetEvent> assetEvents);

    // ── AssetTag ──────────────────────────────────────────────────────────────

    @Mapping(source = "assetItem.houseId", target = "houseId")
    @Mapping(source = "assetItem.id", target = "assetId")
    AssetTagDto tagDto(AssetTag assetTag);

    List<AssetTagDto> tagDtos(List<AssetTag> assetTagDtos);
}
