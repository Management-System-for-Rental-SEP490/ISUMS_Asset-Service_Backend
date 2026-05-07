package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.CreateAssetCategoryRequest;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.UpdateAssetCategoryRequest;
import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetCategoryRepository;
import common.i18n.TranslationMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssetCategoryServiceImpl")
class AssetCategoryServiceImplTest {

    @Mock private AssetCategoryRepository categoryRepository;
    @Mock private AssetMapper assetMapper;
    @Mock private TranslationAutoFillService translationAutoFillService;

    @InjectMocks private AssetCategoryServiceImpl service;

    private UUID id;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
    }

    private AssetCategory category() {
        return AssetCategory.builder()
                .id(id)
                .name(TranslationMap.of(Map.of("vi", "Đồ gỗ", "en", "Wood items", "ja", "木製品")))
                .compensationPercent(50)
                .description(TranslationMap.of(Map.of("vi", "mô tả", "en", "desc", "ja", "説明")))
                .build();
    }

    private AssetCategoryDto dto() {
        return new AssetCategoryDto(
                id,
                "Đồ gỗ",
                Map.of("en", "Wood items", "ja", "木製品"),
                50,
                "mô tả",
                Map.of("en", "desc", "ja", "説明"),
                null
        );
    }

    @Nested
    @DisplayName("createAssetCategory")
    class Create {

        @Test
        @DisplayName("auto-fills translations from Vietnamese-only input")
        void happy() {
            CreateAssetCategoryRequest req = new CreateAssetCategoryRequest(
                    Map.of("vi", "Đồ gỗ"),
                    50,
                    Map.of("vi", "mô tả")
            );
            TranslationMap filledName = TranslationMap.of(Map.of("vi", "Đồ gỗ", "en", "Wood items", "ja", "木製品"));
            TranslationMap filledDesc = TranslationMap.of(Map.of("vi", "mô tả", "en", "desc", "ja", "説明"));
            AssetCategory saved = category();

            when(translationAutoFillService.complete(req.name())).thenReturn(filledName);
            when(translationAutoFillService.complete(req.description())).thenReturn(filledDesc);
            when(categoryRepository.save(any(AssetCategory.class))).thenReturn(saved);
            when(assetMapper.mapAssetCategory(saved)).thenReturn(dto());

            assertThat(service.createAssetCategory(req)).isNotNull();

            ArgumentCaptor<AssetCategory> cap = ArgumentCaptor.forClass(AssetCategory.class);
            verify(categoryRepository).save(cap.capture());
            assertThat(cap.getValue().getName().getTranslations()).containsEntry("en", "Wood items");
            assertThat(cap.getValue().getDescription().getTranslations()).containsEntry("ja", "説明");
        }

        @Test
        @DisplayName("wraps repo exception as RuntimeException")
        void repoFails() {
            CreateAssetCategoryRequest req = new CreateAssetCategoryRequest(
                    Map.of("vi", "n"),
                    10,
                    Map.of("vi", "d")
            );
            when(translationAutoFillService.complete(req.name())).thenReturn(TranslationMap.of(Map.of("vi", "n", "en", "n", "ja", "n")));
            when(translationAutoFillService.complete(req.description())).thenReturn(TranslationMap.of(Map.of("vi", "d", "en", "d", "ja", "d")));
            when(categoryRepository.save(any())).thenThrow(new RuntimeException("db"));

            assertThatThrownBy(() -> service.createAssetCategory(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error to create asset categories");
        }
    }

    @Nested
    @DisplayName("getAllAssetCategories")
    class GetAll {

        @Test
        @DisplayName("returns mapped list")
        void list() {
            when(categoryRepository.findAll()).thenReturn(List.of(category()));
            when(assetMapper.mapAssetCategories(any())).thenReturn(List.of(dto()));

            assertThat(service.getAllAssetCategories()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("returns DTO when found")
        void found() {
            when(categoryRepository.findById(id)).thenReturn(Optional.of(category()));
            when(assetMapper.mapAssetCategory(any())).thenReturn(dto());

            assertThat(service.getById(id)).isNotNull();
        }
    }

    @Nested
    @DisplayName("updateAssetCategory")
    class Update {

        @Test
        @DisplayName("auto-fills missing translations on update")
        void updatesAll() {
            AssetCategory existing = category();
            UpdateAssetCategoryRequest request = new UpdateAssetCategoryRequest(
                    Map.of("vi", "Thiết bị bếp"),
                    80,
                    Map.of("vi", "dùng trong bếp")
            );
            TranslationMap filledName = TranslationMap.of(Map.of("vi", "Thiết bị bếp", "en", "Kitchen appliance", "ja", "キッチン設備"));
            TranslationMap filledDesc = TranslationMap.of(Map.of("vi", "dùng trong bếp", "en", "used in kitchen", "ja", "キッチンで使用"));

            when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
            when(translationAutoFillService.complete(request.name())).thenReturn(filledName);
            when(translationAutoFillService.complete(request.description())).thenReturn(filledDesc);
            when(categoryRepository.save(existing)).thenReturn(existing);
            when(assetMapper.mapAssetCategory(existing)).thenReturn(dto());

            service.updateAssetCategory(id, request);

            assertThat(existing.getName().getTranslations()).containsEntry("en", "Kitchen appliance");
            assertThat(existing.getDescription().getTranslations()).containsEntry("ja", "キッチンで使用");
            assertThat(existing.getCompensationPercent()).isEqualTo(80);
        }

        @Test
        @DisplayName("only updates non-null fields")
        void partial() {
            AssetCategory existing = category();
            when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
            when(categoryRepository.save(existing)).thenReturn(existing);
            when(assetMapper.mapAssetCategory(existing)).thenReturn(dto());

            service.updateAssetCategory(id, new UpdateAssetCategoryRequest(null, null, null));

            verify(translationAutoFillService, never()).complete(any());
        }

        @Test
        @DisplayName("rejects empty name map")
        void blankName() {
            when(categoryRepository.findById(id)).thenReturn(Optional.of(category()));

            assertThatThrownBy(() -> service.updateAssetCategory(id,
                    new UpdateAssetCategoryRequest(Map.of(), null, null)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Name isn't correct form");
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects compensationPercent out of range")
        void tooHighPercent() {
            when(categoryRepository.findById(id)).thenReturn(Optional.of(category()));

            assertThatThrownBy(() -> service.updateAssetCategory(id,
                    new UpdateAssetCategoryRequest(null, 101, null)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("compensation must be in 0 - 100");
        }
    }
}
