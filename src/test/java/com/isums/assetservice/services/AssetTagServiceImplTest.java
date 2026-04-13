package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AssetTagDto.AssetTagDto;
import com.isums.assetservice.domains.dtos.AssetTagDto.AttachTagRequest;
import com.isums.assetservice.domains.dtos.AssetTagDto.TransferTagRequest;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.entities.AssetTag;
import com.isums.assetservice.domains.entities.AssetTagLog;
import com.isums.assetservice.domains.enums.TagType;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetImageRepository;
import com.isums.assetservice.infrastructures.repositories.AssetItemRepository;
import com.isums.assetservice.infrastructures.repositories.AssetTagLogRepository;
import com.isums.assetservice.infrastructures.repositories.AssetTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssetTagServiceImpl")
class AssetTagServiceImplTest {

    @Mock private AssetTagRepository tagRepo;
    @Mock private AssetItemRepository itemRepo;
    @Mock private AssetTagLogRepository tagLogRepo;
    @Mock private AssetImageRepository imageRepo;
    @Mock private AssetMapper mapper;
    @Mock private S3ServiceImpl s3;

    @InjectMocks private AssetTagServiceImpl service;

    private UUID assetId;
    private UUID houseId;

    @BeforeEach
    void setUp() {
        assetId = UUID.randomUUID();
        houseId = UUID.randomUUID();
    }

    private AssetItem asset() {
        return AssetItem.builder().id(assetId).houseId(houseId).build();
    }

    @Nested
    @DisplayName("attachTag")
    class Attach {

        @Test
        @DisplayName("saves new tag and creates ATTACHED log when happy path")
        void happy() {
            AttachTagRequest req = new AttachTagRequest(assetId, "TAG-1", TagType.NFC);
            when(itemRepo.findById(assetId)).thenReturn(Optional.of(asset()));
            when(tagRepo.existsByTagValueAndIsActiveTrue("TAG-1")).thenReturn(false);
            when(tagRepo.existsByAssetItemIdAndTagTypeAndIsActiveTrue(assetId, TagType.NFC)).thenReturn(false);
            when(tagRepo.save(any(AssetTag.class))).thenAnswer(a -> a.getArgument(0));
            AssetTagDto dto = new AssetTagDto(UUID.randomUUID(), "TAG-1", TagType.NFC, assetId, houseId, null, true);
            when(mapper.tagDto(any(AssetTag.class))).thenReturn(dto);

            assertThat(service.attachTag(req)).isNotNull();

            verify(tagLogRepo).save(any(AssetTagLog.class));
        }

        @Test
        @DisplayName("wraps asset-not-found as RuntimeException")
        void assetMissing() {
            when(itemRepo.findById(assetId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.attachTag(
                    new AttachTagRequest(assetId, "TAG-1", TagType.NFC)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error to attach tag");
        }

        @Test
        @DisplayName("rejects tag value already in use")
        void tagInUse() {
            when(itemRepo.findById(assetId)).thenReturn(Optional.of(asset()));
            when(tagRepo.existsByTagValueAndIsActiveTrue("TAG-1")).thenReturn(true);

            assertThatThrownBy(() -> service.attachTag(
                    new AttachTagRequest(assetId, "TAG-1", TagType.NFC)))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("rejects asset already has same tagType active")
        void sameTypeExists() {
            when(itemRepo.findById(assetId)).thenReturn(Optional.of(asset()));
            when(tagRepo.existsByTagValueAndIsActiveTrue("TAG-1")).thenReturn(false);
            when(tagRepo.existsByAssetItemIdAndTagTypeAndIsActiveTrue(assetId, TagType.NFC)).thenReturn(true);

            assertThatThrownBy(() -> service.attachTag(
                    new AttachTagRequest(assetId, "TAG-1", TagType.NFC)))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("detachTag")
    class Detach {

        @Test
        @DisplayName("marks inactive, creates DETACHED log")
        void happy() {
            AssetTag tag = AssetTag.builder()
                    .id(UUID.randomUUID()).tagValue("TAG-1").tagType(TagType.NFC)
                    .isActive(true).assetItem(asset()).build();
            when(tagRepo.findByTagValueAndIsActiveTrue("TAG-1")).thenReturn(Optional.of(tag));
            when(tagRepo.save(tag)).thenReturn(tag);
            when(mapper.tagDto(tag)).thenReturn(new AssetTagDto(
                    tag.getId(), "TAG-1", TagType.NFC, assetId, houseId, null, false));

            service.detachTag("TAG-1");

            assertThat(tag.getIsActive()).isFalse();
            assertThat(tag.getDeactivatedAt()).isNotNull();
            verify(tagLogRepo).save(any(AssetTagLog.class));
        }

        @Test
        @DisplayName("wraps active-tag-missing")
        void missing() {
            when(tagRepo.findByTagValueAndIsActiveTrue("TAG-X")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.detachTag("TAG-X"))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("transferTag")
    class Transfer {

        @Test
        @DisplayName("deactivates old, creates new tag + TRANSFERRED log on happy path")
        void happy() {
            UUID newAssetId = UUID.randomUUID();
            UUID newHouseId = UUID.randomUUID();
            AssetItem newAsset = AssetItem.builder().id(newAssetId).houseId(newHouseId).build();
            AssetTag current = AssetTag.builder()
                    .id(UUID.randomUUID()).tagValue("TAG-1").tagType(TagType.NFC)
                    .isActive(true).assetItem(asset()).build();

            when(tagRepo.findByTagValueAndIsActiveTrue("TAG-1")).thenReturn(Optional.of(current));
            when(itemRepo.findById(newAssetId)).thenReturn(Optional.of(newAsset));
            when(tagRepo.existsByAssetItemIdAndTagTypeAndIsActiveTrue(newAssetId, TagType.NFC)).thenReturn(false);
            when(tagRepo.save(any(AssetTag.class))).thenAnswer(a -> a.getArgument(0));
            when(mapper.tagDto(any(AssetTag.class))).thenReturn(new AssetTagDto(
                    UUID.randomUUID(), "TAG-1", TagType.NFC, newAssetId, newHouseId, null, true));

            service.transferTag(new TransferTagRequest("TAG-1", newAssetId));

            assertThat(current.getIsActive()).isFalse();
            verify(tagLogRepo).save(any(AssetTagLog.class));
        }

        @Test
        @DisplayName("rejects blank tag value")
        void blank() {
            assertThatThrownBy(() -> service.transferTag(
                    new TransferTagRequest("   ", UUID.randomUUID())))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("rejects transfer to same asset")
        void sameAsset() {
            AssetTag current = AssetTag.builder()
                    .id(UUID.randomUUID()).tagValue("TAG-1").tagType(TagType.NFC)
                    .isActive(true).assetItem(asset()).build();

            when(tagRepo.findByTagValueAndIsActiveTrue("TAG-1")).thenReturn(Optional.of(current));
            when(itemRepo.findById(assetId)).thenReturn(Optional.of(asset()));

            assertThatThrownBy(() -> service.transferTag(new TransferTagRequest("TAG-1", assetId)))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("rejects if new asset already has active tag of same type")
        void newAssetHasTag() {
            UUID newAssetId = UUID.randomUUID();
            AssetItem newAsset = AssetItem.builder().id(newAssetId).houseId(UUID.randomUUID()).build();
            AssetTag current = AssetTag.builder()
                    .id(UUID.randomUUID()).tagValue("TAG-1").tagType(TagType.NFC)
                    .isActive(true).assetItem(asset()).build();

            when(tagRepo.findByTagValueAndIsActiveTrue("TAG-1")).thenReturn(Optional.of(current));
            when(itemRepo.findById(newAssetId)).thenReturn(Optional.of(newAsset));
            when(tagRepo.existsByAssetItemIdAndTagTypeAndIsActiveTrue(newAssetId, TagType.NFC)).thenReturn(true);

            assertThatThrownBy(() -> service.transferTag(new TransferTagRequest("TAG-1", newAssetId)))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("getAssetItemByTagValue")
    class GetByTag {

        @Test
        @DisplayName("rejects blank tag value")
        void blank() {
            assertThatThrownBy(() -> service.getAssetItemByTagValue(""))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("wraps when tag not found")
        void notFound() {
            when(tagRepo.findByTagValueAndIsActiveTrue("TAG-X")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAssetItemByTagValue("TAG-X"))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
