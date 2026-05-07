package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.dtos.AssetCountByFunctionAreaDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.CreateAssetItemRequest;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateAssetItemRequest;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateHouseRequest;
import com.isums.assetservice.domains.dtos.BatchUpdateAssetRequest;
import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.domains.entities.AssetEvent;
import com.isums.assetservice.domains.entities.AssetEventImage;
import com.isums.assetservice.domains.entities.AssetImage;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.entities.AssetTag;
import com.isums.assetservice.domains.enums.AssetEventImageType;
import com.isums.assetservice.domains.enums.AssetStatus;
import com.isums.assetservice.infrastructures.grpcs.GrpcUserClient;
import com.isums.assetservice.infrastructures.grpcs.HouseGrpcImpl;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetCategoryRepository;
import com.isums.assetservice.infrastructures.repositories.AssetEventImageRepository;
import com.isums.assetservice.infrastructures.repositories.AssetEventRepository;
import com.isums.assetservice.infrastructures.repositories.AssetImageRepository;
import com.isums.assetservice.infrastructures.repositories.AssetItemRepository;
import com.isums.assetservice.infrastructures.repositories.AssetTagRepository;
import com.isums.houseservice.grpc.FunctionalAreaResponse;
import com.isums.houseservice.grpc.HouseResponse;
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
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    @Mock private GrpcUserClient grpcUserClient;
    @Mock private CachedPageService cachedPageService;
    @Mock private AssetEventImageRepository assetEventImageRepository;
    @Mock private TranslationAutoFillService translationAutoFillService;
    @Mock private HouseGrpcImpl houseGrpc;

    @InjectMocks private AssetItemServiceImpl service;

    @Test
    @DisplayName("create auto-fills translations from Vietnamese-only displayName")
    void createAutoFillsTranslations() {
        UUID houseId = UUID.randomUUID();
        UUID functionAreaId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        AssetCategory category = AssetCategory.builder().id(categoryId).build();
        TranslationMap filled = TranslationMap.of(Map.of("vi", "May lanh", "en", "Air conditioner", "ja", "Air conditioner jp"));
        CreateAssetItemRequest request = new CreateAssetItemRequest(
                houseId,
                functionAreaId,
                categoryId,
                Map.of("vi", "May lanh"),
                "SN-1",
                90,
                AssetStatus.IN_USE,
                List.of()
        );
        AssetItemDto dto = new AssetItemDto();

        when(assetCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(houseGrpc.getHouseById(houseId)).thenReturn(houseResponse(houseId, functionAreaId));
        when(translationAutoFillService.complete(request.displayName())).thenReturn(filled);
        when(assetItemRepository.save(any(AssetItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assetMapper.mapAssetItem(any(AssetItem.class))).thenReturn(dto);

        service.CreateAssetItem(request);

        ArgumentCaptor<AssetItem> captor = ArgumentCaptor.forClass(AssetItem.class);
        verify(assetItemRepository).save(captor.capture());
        assertThat(captor.getValue().getDisplayName().getTranslations()).containsEntry("ja", "Air conditioner jp");
        assertThat(captor.getValue().getFunctionAreaId()).isEqualTo(functionAreaId);
    }

    @Test
    @DisplayName("create rejects function area that does not belong to house")
    void createRejectsForeignFunctionArea() {
        UUID houseId = UUID.randomUUID();
        UUID validAreaId = UUID.randomUUID();
        UUID foreignAreaId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        AssetCategory category = AssetCategory.builder().id(categoryId).build();
        CreateAssetItemRequest request = new CreateAssetItemRequest(
                houseId,
                foreignAreaId,
                categoryId,
                Map.of("vi", "Laptop"),
                "SN-2",
                80,
                AssetStatus.IN_USE,
                List.of()
        );

        when(assetCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(houseGrpc.getHouseById(houseId)).thenReturn(houseResponse(houseId, validAreaId));

        assertThatThrownBy(() -> service.CreateAssetItem(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(foreignAreaId.toString())
                .hasMessageContaining(houseId.toString());

        verify(assetItemRepository, never()).save(any(AssetItem.class));
    }

    @Test
    @DisplayName("update auto-fills translations when only Vietnamese displayName is supplied")
    void updateAutoFillsTranslations() {
        UUID id = UUID.randomUUID();
        UUID houseId = UUID.randomUUID();
        UUID functionAreaId = UUID.randomUUID();
        AssetItem assetItem = AssetItem.builder()
                .id(id)
                .houseId(houseId)
                .displayName(TranslationMap.of(Map.of("vi", "Cu", "en", "Old", "ja", "Old jp")))
                .build();
        TranslationMap filled = TranslationMap.of(Map.of("vi", "May giat", "en", "Washing machine", "ja", "Washing machine jp"));
        UpdateAssetItemRequest request = new UpdateAssetItemRequest(
                functionAreaId,
                Map.of("vi", "May giat"),
                null,
                null,
                null,
                null,
                null
        );
        AssetItemDto dto = new AssetItemDto();

        when(assetItemRepository.findById(id)).thenReturn(Optional.of(assetItem));
        when(houseGrpc.getHouseById(houseId)).thenReturn(houseResponse(houseId, functionAreaId));
        when(translationAutoFillService.complete(request.displayName())).thenReturn(filled);
        when(assetItemRepository.save(assetItem)).thenReturn(assetItem);
        when(assetMapper.mapAssetItem(assetItem)).thenReturn(dto);

        service.UpdateAssetItem(id, request);

        assertThat(assetItem.getDisplayName().getTranslations()).containsEntry("en", "Washing machine");
        assertThat(assetItem.getFunctionAreaId()).isEqualTo(functionAreaId);
    }

    @Test
    @DisplayName("update rejects function area that does not belong to asset house")
    void updateRejectsForeignFunctionArea() {
        UUID id = UUID.randomUUID();
        UUID houseId = UUID.randomUUID();
        UUID validAreaId = UUID.randomUUID();
        UUID foreignAreaId = UUID.randomUUID();
        AssetItem assetItem = AssetItem.builder()
                .id(id)
                .houseId(houseId)
                .build();
        UpdateAssetItemRequest request = new UpdateAssetItemRequest(
                foreignAreaId,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(assetItemRepository.findById(id)).thenReturn(Optional.of(assetItem));
        when(houseGrpc.getHouseById(houseId)).thenReturn(houseResponse(houseId, validAreaId));

        assertThatThrownBy(() -> service.UpdateAssetItem(id, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(foreignAreaId.toString())
                .hasMessageContaining(houseId.toString());

        verify(assetItemRepository, never()).save(assetItem);
    }

    @Test
    @DisplayName("updateHouseForAsset clears function area when asset moves to another house")
    void updateHouseClearsFunctionArea() {
        UUID assetId = UUID.randomUUID();
        UUID oldHouseId = UUID.randomUUID();
        UUID newHouseId = UUID.randomUUID();
        UUID functionAreaId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AssetItem assetItem = AssetItem.builder()
                .id(assetId)
                .houseId(oldHouseId)
                .functionAreaId(functionAreaId)
                .build();
        AssetItemDto dto = new AssetItemDto();

        when(assetItemRepository.findById(assetId)).thenReturn(Optional.of(assetItem));
        when(assetItemRepository.save(assetItem)).thenReturn(assetItem);
        when(assetEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(assetMapper.mapAssetItem(assetItem)).thenReturn(dto);

        service.updateHouseForAsset(assetId, new UpdateHouseRequest(newHouseId), userId);

        assertThat(assetItem.getHouseId()).isEqualTo(newHouseId);
        assertThat(assetItem.getFunctionAreaId()).isNull();
        verify(assetItemRepository).save(assetItem);
    }

    @Test
    @DisplayName("getAssetItemById returns category detail in response")
    void getAssetItemByIdIncludesCategoryDetail() {
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
        categoryDto.setName("Dieu hoa");

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
        assertThat(result.getCategory().getName()).isEqualTo("Dieu hoa");
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

    @Test
    @DisplayName("batch maintenance snapshots current asset images as old images")
    void batchMaintenanceSnapshotsCurrentImagesAsOldImages() {
        UUID assetId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID maintenanceEventId = UUID.randomUUID();
        Instant eventAt = Instant.parse("2026-04-25T07:59:20Z");

        AssetItem asset = AssetItem.builder()
                .id(assetId)
                .conditionPercent(90)
                .build();
        AssetImage currentImageOne = AssetImage.builder()
                .id(UUID.randomUUID())
                .assetItem(asset)
                .key("asset/current-1.jpg")
                .createdAt(eventAt.minusSeconds(120))
                .build();
        AssetImage currentImageTwo = AssetImage.builder()
                .id(UUID.randomUUID())
                .assetItem(asset)
                .key("asset/current-2.jpg")
                .createdAt(eventAt.minusSeconds(60))
                .build();
        BatchUpdateAssetRequest request = new BatchUpdateAssetRequest(
                jobId,
                List.of(new BatchUpdateAssetRequest.AssetUpdateItem(assetId, 95, "Tot", null))
        );

        when(assetItemRepository.findAllById(List.of(assetId))).thenReturn(List.of(asset));
        when(translationAutoFillService.complete(Map.of("vi", "Tot")))
                .thenReturn(TranslationMap.of(Map.of("vi", "Tot")));
        when(assetEventRepository.save(any(AssetEvent.class))).thenAnswer(invocation -> {
            AssetEvent event = invocation.getArgument(0);
            event.setId(maintenanceEventId);
            event.setCreatedAt(eventAt);
            return event;
        });
        when(assetImageRepository.findByAssetItemId(assetId)).thenReturn(List.of(currentImageOne, currentImageTwo));

        service.batchUpdateAssetCondition(UUID.randomUUID(), request);

        ArgumentCaptor<List<AssetEventImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(assetEventImageRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(AssetEventImage::getType, AssetEventImage::getKey)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(AssetEventImageType.BEFORE, "asset/current-1.jpg"),
                        org.assertj.core.groups.Tuple.tuple(AssetEventImageType.BEFORE, "asset/current-2.jpg")
                );
    }

    @Test
    @DisplayName("batch maintenance does not clone recent asset image replacement")
    void batchMaintenanceDoesNotCloneRecentImageReplacement() {
        UUID assetId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID maintenanceEventId = UUID.randomUUID();
        Instant eventAt = Instant.parse("2026-04-25T08:10:00Z");

        AssetItem asset = AssetItem.builder()
                .id(assetId)
                .conditionPercent(80)
                .build();
        AssetImage currentNewImage = AssetImage.builder()
                .id(UUID.randomUUID())
                .assetItem(asset)
                .key("asset/new-after.jpg")
                .createdAt(eventAt.minusSeconds(20))
                .build();
        BatchUpdateAssetRequest request = new BatchUpdateAssetRequest(
                jobId,
                List.of(new BatchUpdateAssetRequest.AssetUpdateItem(assetId, 85, "Checked", null))
        );

        when(assetItemRepository.findAllById(List.of(assetId))).thenReturn(List.of(asset));
        when(translationAutoFillService.complete(Map.of("vi", "Checked")))
                .thenReturn(TranslationMap.of(Map.of("vi", "Checked")));
        when(assetEventRepository.save(any(AssetEvent.class))).thenAnswer(invocation -> {
            AssetEvent event = invocation.getArgument(0);
            event.setId(maintenanceEventId);
            event.setCreatedAt(eventAt);
            return event;
        });
        when(assetImageRepository.findByAssetItemId(assetId)).thenReturn(List.of(currentNewImage));

        service.batchUpdateAssetCondition(UUID.randomUUID(), request);

        ArgumentCaptor<List<AssetEventImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(assetEventImageRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(AssetEventImage::getType, AssetEventImage::getKey)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(AssetEventImageType.BEFORE, "asset/new-after.jpg")
                );
        verify(assetEventRepository, never()).findRecentUnscopedImageReplacementEvents(any(), any(), any(), any());
    }

    @Test
    @DisplayName("confirmAsset moves waiting asset to IN_USE without role lookup")
    void confirmAssetAllowsWaitingAssetWithoutRoleLookup() {
        UUID assetId = UUID.randomUUID();
        AssetItem asset = AssetItem.builder()
                .id(assetId)
                .status(AssetStatus.WAITING_MANAGER_CONFIRM)
                .build();
        AssetItemDto dto = new AssetItemDto();

        when(assetItemRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(assetItemRepository.save(asset)).thenReturn(asset);
        when(assetMapper.mapAssetItem(asset)).thenReturn(dto);

        AssetItemDto result = service.confirmAsset(assetId, AssetStatus.IN_USE);

        assertThat(result).isSameAs(dto);
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.IN_USE);
        verify(assetItemRepository).save(asset);
        verify(grpcUserClient, never()).getUserIdAndRoleByKeyCloakId(any());
    }

    @Test
    @DisplayName("confirmAsset rejects assets outside waiting manager confirm state")
    void confirmAssetRejectsWrongState() {
        UUID assetId = UUID.randomUUID();
        AssetItem asset = AssetItem.builder()
                .id(assetId)
                .status(AssetStatus.IN_USE)
                .build();

        when(assetItemRepository.findById(assetId)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.confirmAsset(assetId, AssetStatus.DELETED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Asset is not in waiting state");

        verify(assetItemRepository, never()).save(any());
        verify(grpcUserClient, never()).getUserIdAndRoleByKeyCloakId(any());
    }

    private HouseResponse houseResponse(UUID houseId, UUID... functionAreaIds) {
        HouseResponse.Builder builder = HouseResponse.newBuilder().setId(houseId.toString());
        for (UUID functionAreaId : functionAreaIds) {
            builder.addFunctionalAreas(
                    FunctionalAreaResponse.newBuilder()
                            .setId(functionAreaId.toString())
                            .build()
            );
        }
        return builder.build();
    }
}
