package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.dtos.AssetCountByFunctionAreaDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.CreateAssetItemRequest;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateAssetItemRequest;
import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.domains.entities.AssetImage;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.entities.AssetTag;
import com.isums.assetservice.domains.enums.AssetStatus;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetCategoryRepository;
import com.isums.assetservice.infrastructures.repositories.AssetEventImageRepository;
import com.isums.assetservice.infrastructures.repositories.AssetEventRepository;
import com.isums.assetservice.infrastructures.repositories.AssetImageRepository;
import com.isums.assetservice.infrastructures.repositories.AssetItemRepository;
import com.isums.assetservice.infrastructures.repositories.AssetTagRepository;
import common.i18n.TranslationMap;
import common.paginations.cache.CachedPageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssetItemServiceImpl")
class AssetItemServiceImplTest {

    @Mock private AssetCategoryRepository assetCategoryRepository;
    @Mock private AssetEventRepository assetEventRepository;
    @Mock private AssetMapper assetMapper;
    @Mock private AssetItemRepository assetItemRepository;
    @Mock private AssetTagRepository assetTagRepository;
    @Mock private S3ServiceImpl s3;
    @Mock private AssetImageRepository assetImageRepository;
    @Mock private com.isums.assetservice.infrastructures.grpcs.GrpcUserClient grpcUserClient;
    @Mock private CachedPageService cachedPageService;
    @Mock private AssetEventImageRepository assetEventImageRepository;
    @Mock private TranslationAutoFillService translationAutoFillService;

    @InjectMocks private AssetItemServiceImpl service;

    @Test
    @DisplayName("create auto-fills translations from Vietnamese-only displayName")
    void createAutoFillsTranslations() {
        UUID categoryId = UUID.randomUUID();
        AssetCategory category = AssetCategory.builder().id(categoryId).build();
        TranslationMap filled = TranslationMap.of(Map.of("vi", "Máy lạnh", "en", "Air conditioner", "ja", "エアコン"));
        CreateAssetItemRequest request = new CreateAssetItemRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                categoryId,
                Map.of("vi", "Máy lạnh"),
                "SN-1",
                90,
                AssetStatus.IN_USE,
                List.of()
        );
        AssetItemDto dto = new AssetItemDto();

        when(assetCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(translationAutoFillService.complete(request.displayName())).thenReturn(filled);
        when(assetItemRepository.save(any(AssetItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assetMapper.mapAssetItem(any(AssetItem.class))).thenReturn(dto);

        service.CreateAssetItem(request);

        ArgumentCaptor<AssetItem> captor = ArgumentCaptor.forClass(AssetItem.class);
        verify(assetItemRepository).save(captor.capture());
        assertThat(captor.getValue().getDisplayName().getTranslations()).containsEntry("ja", "エアコン");
    }

    @Test
    @DisplayName("update auto-fills translations when only Vietnamese displayName is supplied")
    void updateAutoFillsTranslations() {
        UUID id = UUID.randomUUID();
        AssetItem assetItem = AssetItem.builder()
                .id(id)
                .displayName(TranslationMap.of(Map.of("vi", "Cũ", "en", "Old", "ja", "古い")))
                .build();
        TranslationMap filled = TranslationMap.of(Map.of("vi", "Máy giặt", "en", "Washing machine", "ja", "洗濯機"));
        UpdateAssetItemRequest request = new UpdateAssetItemRequest(
                null,
                Map.of("vi", "Máy giặt"),
                null,
                null,
                null,
                null,
                null
        );
        AssetItemDto dto = new AssetItemDto();

        when(assetItemRepository.findById(id)).thenReturn(Optional.of(assetItem));
        when(translationAutoFillService.complete(request.displayName())).thenReturn(filled);
        when(assetItemRepository.save(assetItem)).thenReturn(assetItem);
        when(assetMapper.mapAssetItem(assetItem)).thenReturn(dto);

        service.UpdateAssetItem(id, request);

        assertThat(assetItem.getDisplayName().getTranslations()).containsEntry("en", "Washing machine");
    }

    @Test
    @DisplayName("getAssetItemById returns category detail in response")
    void getAssetItemById_includesCategoryDetail() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        AssetCategory category = AssetCategory.builder()
                .id(categoryId)
                .build();

        AssetItem assetItem = AssetItem.builder()
                .id(id)
                .category(category)
                .build();

        AssetCategoryDto categoryDto = new AssetCategoryDto();
        categoryDto.setId(categoryId);
        categoryDto.setName("Điều hòa");

        AssetItemDto dto = new AssetItemDto();
        dto.setId(id);
        dto.setCategoryId(categoryId);
        dto.setCategory(categoryDto);

        when(assetItemRepository.findById(id)).thenReturn(Optional.of(assetItem));
        when(assetMapper.mapAssetItem(assetItem)).thenReturn(dto);
        when(assetTagRepository.findByAssetItemIdAndIsActiveTrue(id)).thenReturn(List.of());
        when(assetImageRepository.findByAssetItemId(id)).thenReturn(List.of());

        AssetItemDto result = service.getAssetItemById(id);

        assertThat(result.getCategoryId()).isEqualTo(categoryId);
        assertThat(result.getCategory()).isNotNull();
        assertThat(result.getCategory().getId()).isEqualTo(categoryId);
        assertThat(result.getCategory().getName()).isEqualTo("Điều hòa");
    }

    @Test
    @DisplayName("getAssetItemsByHouseIdAndFunctionAreaId filters by both house and function area, then hydrates tags/images")
    void getByHouseAndFunctionArea() {
        UUID houseId = UUID.randomUUID();
        UUID functionAreaId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();

        AssetCategory category = AssetCategory.builder().id(UUID.randomUUID()).build();
        AssetItem asset = AssetItem.builder()
                .id(assetId)
                .houseId(houseId)
                .functionAreaId(functionAreaId)
                .category(category)
                .displayName(TranslationMap.of(Map.of("vi", "May lanh")))
                .build();

        AssetTag tag = AssetTag.builder()
                .id(tagId)
                .assetItem(asset)
                .build();

        AssetImage image = AssetImage.builder()
                .id(imageId)
                .assetItem(asset)
                .key("asset/test.jpg")
                .build();

        AssetItemDto dto = new AssetItemDto();
        dto.setId(assetId);
        dto.setHouseId(houseId);
        dto.setFunctionAreaId(functionAreaId);

        when(assetItemRepository.findByHouseIdAndFunctionAreaId(houseId, functionAreaId)).thenReturn(List.of(asset));
        when(assetTagRepository.findByAssetItemIdInAndIsActiveTrue(List.of(assetId))).thenReturn(List.of(tag));
        when(assetMapper.mapAssetItem(asset)).thenReturn(dto);
        when(assetMapper.tagDtos(List.of(tag))).thenReturn(List.of());
        when(assetImageRepository.findByAssetItemId(assetId)).thenReturn(List.of(image));
        when(s3.getImageUrl("asset/test.jpg")).thenReturn("https://isums.pro/asset/test.jpg");

        List<AssetItemDto> result = service.getAssetItemsByHouseIdAndFunctionAreaId(houseId, functionAreaId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(assetId);
        assertThat(result.getFirst().getFunctionAreaId()).isEqualTo(functionAreaId);
        assertThat(result.getFirst().getImages())
                .extracting(com.isums.assetservice.domains.dtos.AssetImageDto::url)
                .containsExactly("https://isums.pro/asset/test.jpg");
        verify(assetItemRepository).findByHouseIdAndFunctionAreaId(houseId, functionAreaId);
        verify(assetTagRepository).findByAssetItemIdInAndIsActiveTrue(List.of(assetId));
        verify(assetMapper).mapAssetItem(asset);
        verify(assetMapper).tagDtos(eq(List.of(tag)));
    }

    @Test
    @DisplayName("getAssetCountByFunctionArea returns grouped counts from repository")
    void getAssetCountByFunctionArea() {
        UUID houseId = UUID.randomUUID();
        UUID areaA = UUID.randomUUID();
        UUID areaB = UUID.randomUUID();
        List<AssetCountByFunctionAreaDto> counts = List.of(
                new AssetCountByFunctionAreaDto(areaA, 2),
                new AssetCountByFunctionAreaDto(areaB, 1)
        );

        when(assetItemRepository.countByHouseIdGroupByFunctionAreaId(houseId)).thenReturn(counts);

        assertThat(service.getAssetCountByFunctionArea(houseId)).isEqualTo(counts);
        verify(assetItemRepository).countByHouseIdGroupByFunctionAreaId(houseId);
    }
}
