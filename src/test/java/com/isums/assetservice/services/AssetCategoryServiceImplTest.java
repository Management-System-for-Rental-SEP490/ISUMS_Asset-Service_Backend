package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.CreateAssetCategoryRequest;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.UpdateAssetCategoryRequest;
import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetCategoryRepository;
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

    @InjectMocks private AssetCategoryServiceImpl service;

    private UUID id;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
    }

    private AssetCategory category() {
        return AssetCategory.builder().id(id).name("Đồ gỗ").compensationPercent(50).description("desc").build();
    }

    private AssetCategoryDto dto() {
        return new AssetCategoryDto(id, "Đồ gỗ", 50, "desc", null);
    }

    @Nested
    @DisplayName("createAssetCategory")
    class Create {

        @Test
        @DisplayName("saves and returns DTO on happy path")
        void happy() {
            CreateAssetCategoryRequest req = new CreateAssetCategoryRequest("Đồ gỗ", 50, "desc");
            AssetCategory saved = category();
            when(categoryRepository.save(any(AssetCategory.class))).thenReturn(saved);
            when(assetMapper.mapAssetCategory(saved)).thenReturn(dto());

            assertThat(service.createAssetCategory(req)).isNotNull();

            ArgumentCaptor<AssetCategory> cap = ArgumentCaptor.forClass(AssetCategory.class);
            verify(categoryRepository).save(cap.capture());
            assertThat(cap.getValue().getName()).isEqualTo("Đồ gỗ");
            assertThat(cap.getValue().getCompensationPercent()).isEqualTo(50);
        }

        @Test
        @DisplayName("wraps repo exception as RuntimeException (pre-existing pattern)")
        void repoFails() {
            when(categoryRepository.save(any())).thenThrow(new RuntimeException("db"));

            assertThatThrownBy(() -> service.createAssetCategory(
                    new CreateAssetCategoryRequest("n", 10, "d")))
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

        @Test
        @DisplayName("wraps repo exception")
        void wraps() {
            when(categoryRepository.findAll()).thenThrow(new RuntimeException("db"));

            assertThatThrownBy(() -> service.getAllAssetCategories())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error to get all asset categories");
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

        @Test
        @DisplayName("wraps not-found into RuntimeException (double-wrapped by catch)")
        void missing() {
            when(categoryRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(id))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Can't get categories by id");
        }
    }

    @Nested
    @DisplayName("updateAssetCategory")
    class Update {

        @Test
        @DisplayName("updates name + compensationPercent + description on happy path")
        void updatesAll() {
            AssetCategory existing = category();
            when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
            when(categoryRepository.save(existing)).thenReturn(existing);
            when(assetMapper.mapAssetCategory(existing)).thenReturn(dto());

            service.updateAssetCategory(id, new UpdateAssetCategoryRequest("New", 80, "New desc"));

            assertThat(existing.getName()).isEqualTo("New");
            assertThat(existing.getCompensationPercent()).isEqualTo(80);
            assertThat(existing.getDescription()).isEqualTo("New desc");
        }

        @Test
        @DisplayName("only updates non-null fields")
        void partial() {
            AssetCategory existing = category();
            when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
            when(categoryRepository.save(existing)).thenReturn(existing);
            when(assetMapper.mapAssetCategory(existing)).thenReturn(dto());

            service.updateAssetCategory(id, new UpdateAssetCategoryRequest(null, null, "Only desc"));

            assertThat(existing.getName()).isEqualTo("Đồ gỗ");
            assertThat(existing.getCompensationPercent()).isEqualTo(50);
            assertThat(existing.getDescription()).isEqualTo("Only desc");
        }

        @Test
        @DisplayName("rejects blank name")
        void blankName() {
            when(categoryRepository.findById(id)).thenReturn(Optional.of(category()));

            assertThatThrownBy(() -> service.updateAssetCategory(id,
                    new UpdateAssetCategoryRequest("   ", null, null)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Name isn't correct form");
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects compensationPercent out of range (<0)")
        void negativePercent() {
            when(categoryRepository.findById(id)).thenReturn(Optional.of(category()));

            assertThatThrownBy(() -> service.updateAssetCategory(id,
                    new UpdateAssetCategoryRequest(null, -1, null)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("compensation must be in 0 - 100");
        }

        @Test
        @DisplayName("rejects compensationPercent out of range (>100)")
        void tooHighPercent() {
            when(categoryRepository.findById(id)).thenReturn(Optional.of(category()));

            assertThatThrownBy(() -> service.updateAssetCategory(id,
                    new UpdateAssetCategoryRequest(null, 101, null)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("compensation must be in 0 - 100");
        }

        @Test
        @DisplayName("wraps not-found as RuntimeException")
        void missing() {
            when(categoryRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateAssetCategory(id,
                    new UpdateAssetCategoryRequest("n", 10, "d")))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("deleteAssetCategory")
    class Delete {

        @Test
        @DisplayName("returns null (unimplemented) — test documents current behaviour")
        void returnsNull() {
            assertThat(service.deleteAssetCategory(id)).isNull();
        }
    }
}
