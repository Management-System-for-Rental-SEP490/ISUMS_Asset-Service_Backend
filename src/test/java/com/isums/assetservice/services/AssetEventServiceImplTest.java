package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.dtos.AssetEventDTO.UpdateAssetEventRequest;
import com.isums.assetservice.domains.entities.AssetEvent;
import com.isums.assetservice.domains.entities.AssetEventImage;
import com.isums.assetservice.domains.entities.AssetImage;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.enums.AssetEventImageType;
import com.isums.assetservice.domains.enums.AssetEventType;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetEventImageRepository;
import com.isums.assetservice.infrastructures.repositories.AssetEventRepository;
import com.isums.assetservice.infrastructures.repositories.AssetImageRepository;
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
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssetEventServiceImpl")
class AssetEventServiceImplTest {

    @Mock private AssetEventRepository eventRepo;
    @Mock private AssetEventImageRepository eventImageRepo;
    @Mock private AssetMapper mapper;
    @Mock private S3ServiceImpl s3;
    @Mock private AssetImageRepository imageRepo;

    @InjectMocks private AssetEventServiceImpl service;

    private UUID id;
    private AssetItem assetItem;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        assetItem = AssetItem.builder()
                .id(UUID.randomUUID())
                .displayName(TranslationMap.ofDefault("Cai quat"))
                .build();
    }

    private AssetEvent event() {
        return AssetEvent.builder()
                .id(id)
                .eventType(AssetEventType.CREATED)
                .assetItem(assetItem)
                .createdAt(Instant.now())
                .build();
    }

    private AssetEventDto dto() {
        AssetEventDto d = new AssetEventDto();
        d.setId(id);
        d.setEventType(AssetEventType.CREATED);
        return d;
    }

    @Nested
    @DisplayName("getAllAssetEvents")
    class GetAll {

        @Test
        @DisplayName("returns mapped events on happy path")
        void happy() {
            when(eventRepo.findAll()).thenReturn(List.of(event()));
            when(mapper.maAssetEvents(any())).thenReturn(List.of(dto()));

            assertThat(service.getAllAssetEvents()).hasSize(1);
        }

        @Test
        @DisplayName("wraps repo failure")
        void wraps() {
            when(eventRepo.findAll()).thenThrow(new RuntimeException("db"));

            assertThatThrownBy(() -> service.getAllAssetEvents())
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("updateEventStatus")
    class UpdateStatus {

        @Test
        @DisplayName("updates eventType and saves when status provided")
        void happy() {
            AssetEvent e = event();
            when(eventRepo.findById(id)).thenReturn(Optional.of(e));
            when(eventRepo.save(e)).thenReturn(e);
            when(mapper.mapAssetEvent(e)).thenReturn(dto());

            service.updateEventStatus(id, new UpdateAssetEventRequest(AssetEventType.REPAIRED));

            assertThat(e.getEventType()).isEqualTo(AssetEventType.REPAIRED);
            verify(eventRepo).save(e);
        }

        @Test
        @DisplayName("wraps when event missing")
        void missing() {
            when(eventRepo.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateEventStatus(id,
                    new UpdateAssetEventRequest(AssetEventType.REPAIRED)))
                    .isInstanceOf(RuntimeException.class);
            verify(eventRepo, never()).save(any());
        }

        @Test
        @DisplayName("wraps when status null")
        void nullStatus() {
            when(eventRepo.findById(id)).thenReturn(Optional.of(event()));

            assertThatThrownBy(() -> service.updateEventStatus(id,
                    new UpdateAssetEventRequest(null)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Status is required");
        }
    }

    @Nested
    @DisplayName("getEventsByJob")
    class GetEventsByJob {

        @Test
        @DisplayName("maps before and after images from the same event")
        void mapsBeforeAndAfterImagesFromCurrentEvent() {
            UUID jobId = UUID.randomUUID();
            AssetEvent e = event();
            AssetEventImage beforeImage = AssetEventImage.builder()
                    .id(UUID.randomUUID())
                    .event(e)
                    .key("asset/before.jpg")
                    .type(AssetEventImageType.BEFORE)
                    .createdAt(Instant.now().minusSeconds(60))
                    .build();
            AssetEventImage afterImage = AssetEventImage.builder()
                    .id(UUID.randomUUID())
                    .event(e)
                    .key("asset/after.jpg")
                    .type(AssetEventImageType.AFTER)
                    .createdAt(Instant.now())
                    .build();
            AssetEventDto mapped = dto();

            when(eventRepo.findByJobIdWithAsset(jobId)).thenReturn(List.of(e));
            when(mapper.mapAssetEvent(e)).thenReturn(mapped);
            when(eventImageRepo.findByEventIdAndTypeOrderByCreatedAtAsc(e.getId(), AssetEventImageType.BEFORE))
                    .thenReturn(List.of(beforeImage));
            when(eventImageRepo.findByEventIdAndTypeOrderByCreatedAtAsc(e.getId(), AssetEventImageType.AFTER))
                    .thenReturn(List.of(afterImage));
            when(s3.getImageUrl("asset/before.jpg")).thenReturn("https://isums.pro/asset/before.jpg");
            when(s3.getImageUrl("asset/after.jpg")).thenReturn("https://isums.pro/asset/after.jpg");

            List<AssetEventDto> result = service.getEventsByJob(jobId);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getOldImages())
                    .extracting("url")
                    .containsExactly("https://isums.pro/asset/before.jpg");
            assertThat(result.getFirst().getImages())
                    .extracting("url")
                    .containsExactly("https://isums.pro/asset/after.jpg");
            verify(eventRepo, never()).findPreviousEvent(any());
        }

        @Test
        @DisplayName("keeps after image even when content hash matches before image")
        void keepsAfterImageWhenContentDuplicatesBeforeImage() {
            UUID jobId = UUID.randomUUID();
            AssetEvent e = event();
            AssetEventImage beforeImage = AssetEventImage.builder()
                    .id(UUID.randomUUID())
                    .event(e)
                    .key("asset/before.jpg")
                    .type(AssetEventImageType.BEFORE)
                    .createdAt(Instant.now().minusSeconds(60))
                    .build();
            AssetEventImage duplicateAfterImage = AssetEventImage.builder()
                    .id(UUID.randomUUID())
                    .event(e)
                    .key("asset/after-same.jpg")
                    .type(AssetEventImageType.AFTER)
                    .createdAt(Instant.now())
                    .build();
            AssetEventDto mapped = dto();
            String sameHash = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

            when(eventRepo.findByJobIdWithAsset(jobId)).thenReturn(List.of(e));
            when(mapper.mapAssetEvent(e)).thenReturn(mapped);
            when(eventImageRepo.findByEventIdAndTypeOrderByCreatedAtAsc(e.getId(), AssetEventImageType.BEFORE))
                    .thenReturn(List.of(beforeImage));
            when(eventImageRepo.findByEventIdAndTypeOrderByCreatedAtAsc(e.getId(), AssetEventImageType.AFTER))
                    .thenReturn(List.of(duplicateAfterImage));
            when(s3.getImageUrl("asset/before.jpg")).thenReturn("https://isums.pro/asset/before.jpg");
            when(s3.getImageUrl("asset/after-same.jpg")).thenReturn("https://isums.pro/asset/after-same.jpg");

            List<AssetEventDto> result = service.getEventsByJob(jobId);

            assertThat(result.getFirst().getOldImages())
                    .extracting("url")
                    .containsExactly("https://isums.pro/asset/before.jpg");
            assertThat(result.getFirst().getImages())
                    .extracting("url")
                    .containsExactly("https://isums.pro/asset/after-same.jpg");
        }

        @Test
        @DisplayName("does not borrow images from recent unscoped asset uploads")
        void doesNotBorrowRecentUnscopedUploadImages() {
            UUID jobId = UUID.randomUUID();
            AssetEvent maintenance = event();
            maintenance.setJobId(jobId);
            maintenance.setEventType(AssetEventType.MAINTENANCE);
            AssetEventImage beforeImage = AssetEventImage.builder()
                    .id(UUID.randomUUID())
                    .event(maintenance)
                    .key("asset/current-at-batch.jpg")
                    .type(AssetEventImageType.BEFORE)
                    .createdAt(Instant.now().minusSeconds(60))
                    .build();
            AssetEventImage afterImage = AssetEventImage.builder()
                    .id(UUID.randomUUID())
                    .event(maintenance)
                    .key("asset/uploaded-after-batch.jpg")
                    .type(AssetEventImageType.AFTER)
                    .createdAt(Instant.now())
                    .build();
            AssetEventDto mapped = dto();

            when(eventRepo.findByJobIdWithAsset(jobId)).thenReturn(List.of(maintenance));
            when(mapper.mapAssetEvent(maintenance)).thenReturn(mapped);
            when(eventImageRepo.findByEventIdAndTypeOrderByCreatedAtAsc(id, AssetEventImageType.BEFORE))
                    .thenReturn(List.of(beforeImage));
            when(eventImageRepo.findByEventIdAndTypeOrderByCreatedAtAsc(id, AssetEventImageType.AFTER))
                    .thenReturn(List.of(afterImage));
            when(s3.getImageUrl("asset/current-at-batch.jpg")).thenReturn("https://isums.pro/asset/current-at-batch.jpg");
            when(s3.getImageUrl("asset/uploaded-after-batch.jpg")).thenReturn("https://isums.pro/asset/uploaded-after-batch.jpg");

            List<AssetEventDto> result = service.getEventsByJob(jobId);

            assertThat(result.getFirst().getOldImages())
                    .extracting("url")
                    .containsExactly("https://isums.pro/asset/current-at-batch.jpg");
            assertThat(result.getFirst().getImages())
                    .extracting("url")
                    .containsExactly("https://isums.pro/asset/uploaded-after-batch.jpg");
        }

        @Test
        @DisplayName("does not rewrite maintenance image groups from recent asset upload events")
        void doesNotRewriteMaintenanceImageGroupsFromRecentAssetUploadEvents() {
            UUID jobId = UUID.randomUUID();
            Instant maintenanceAt = Instant.parse("2026-04-25T11:47:01Z");
            AssetEvent maintenance = event();
            maintenance.setJobId(jobId);
            maintenance.setEventType(AssetEventType.MAINTENANCE);
            maintenance.setCreatedAt(maintenanceAt);
            AssetEventImage wronglyPersistedOldImage = AssetEventImage.builder()
                    .id(UUID.randomUUID())
                    .event(maintenance)
                    .key("asset/first-upload.jpg")
                    .type(AssetEventImageType.BEFORE)
                    .createdAt(maintenanceAt.minusSeconds(264))
                    .build();
            AssetEventDto mapped = dto();

            when(eventRepo.findByJobIdWithAsset(jobId)).thenReturn(List.of(maintenance));
            when(mapper.mapAssetEvent(maintenance)).thenReturn(mapped);
            when(eventImageRepo.findByEventIdAndTypeOrderByCreatedAtAsc(id, AssetEventImageType.BEFORE))
                    .thenReturn(List.of(wronglyPersistedOldImage));
            when(eventImageRepo.findByEventIdAndTypeOrderByCreatedAtAsc(id, AssetEventImageType.AFTER))
                    .thenReturn(List.of());
            when(s3.getImageUrl("asset/first-upload.jpg")).thenReturn("https://isums.pro/asset/first-upload.jpg");

            List<AssetEventDto> result = service.getEventsByJob(jobId);

            assertThat(result.getFirst().getOldImages())
                    .extracting("url")
                    .containsExactly("https://isums.pro/asset/first-upload.jpg");
            assertThat(result.getFirst().getImages()).isEmpty();
            verify(eventRepo, never()).findRecentUnscopedImageReplacementEvents(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getLatestEvent")
    class Latest {

        @Test
        @DisplayName("falls back to legacy untyped images for current image list")
        void fallsBackToLegacyUntypedAfterImages() {
            AssetEvent e = event();
            AssetEventImage legacyImage = AssetEventImage.builder()
                    .id(UUID.randomUUID())
                    .event(e)
                    .key("asset/current.jpg")
                    .createdAt(Instant.now())
                    .build();

            when(eventRepo.findLatestEvent(assetItem.getId())).thenReturn(List.of(e));
            when(eventImageRepo.findByEventIdAndTypeOrderByCreatedAtAsc(id, AssetEventImageType.BEFORE))
                    .thenReturn(List.of());
            when(eventImageRepo.findByEventIdAndTypeOrderByCreatedAtAsc(id, AssetEventImageType.AFTER))
                    .thenReturn(List.of());
            when(eventImageRepo.findByEventIdOrderByCreatedAtAsc(id)).thenReturn(List.of(legacyImage));
            when(s3.getImageUrl("asset/current.jpg")).thenReturn("https://isums.pro/asset/current.jpg");

            AssetEventDto result = service.getLatestEvent(assetItem.getId());

            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getImages())
                    .extracting("url")
                    .containsExactly("https://isums.pro/asset/current.jpg");
            assertThat(result.getOldImages()).isEmpty();
        }
    }

    @Nested
    @DisplayName("uploadEventImages")
    class Upload {

        @Test
        @DisplayName("snapshots current asset images as BEFORE and uploads new ones as AFTER")
        void snapshotsBeforeAndUploadsAfter() {
            AssetEvent e = event();
            AssetImage currentOne = AssetImage.builder()
                    .id(UUID.randomUUID())
                    .assetItem(assetItem)
                    .key("asset/current-1.jpg")
                    .createdAt(Instant.now().minusSeconds(120))
                    .build();
            AssetImage currentTwo = AssetImage.builder()
                    .id(UUID.randomUUID())
                    .assetItem(assetItem)
                    .key("asset/current-2.jpg")
                    .createdAt(Instant.now().minusSeconds(60))
                    .build();
            MockMultipartFile newFile = new MockMultipartFile("files", "new.jpg", "image/jpeg", "abc".getBytes());

            when(eventRepo.findById(id)).thenReturn(Optional.of(e));
            when(imageRepo.findByAssetItemId(assetItem.getId())).thenReturn(List.of(currentOne, currentTwo));
            when(eventImageRepo.findByEventIdAndTypeOrderByCreatedAtAsc(id, AssetEventImageType.BEFORE))
                    .thenReturn(List.of());
            when(eventImageRepo.findByEventIdAndTypeOrderByCreatedAtAsc(id, AssetEventImageType.AFTER))
                    .thenReturn(List.of());
            when(s3.upload(newFile, "asset/" + assetItem.getId())).thenReturn("asset/new.jpg");

            service.uploadEventImages(id, List.of(newFile));

            ArgumentCaptor<List<AssetEventImage>> beforeCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<AssetEventImage> afterCaptor = ArgumentCaptor.forClass(AssetEventImage.class);

            verify(eventImageRepo).saveAll(beforeCaptor.capture());
            verify(eventImageRepo).save(afterCaptor.capture());
            verify(imageRepo).deleteAll(List.of(currentOne, currentTwo));

            assertThat(beforeCaptor.getValue())
                    .extracting(AssetEventImage::getType, AssetEventImage::getKey)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(AssetEventImageType.BEFORE, "asset/current-1.jpg"),
                            org.assertj.core.groups.Tuple.tuple(AssetEventImageType.BEFORE, "asset/current-2.jpg")
                    );
            assertThat(afterCaptor.getValue().getType()).isEqualTo(AssetEventImageType.AFTER);
            assertThat(afterCaptor.getValue().getKey()).isEqualTo("asset/new.jpg");
        }

        @Test
        @DisplayName("throws when event missing")
        void eventMissing() {
            UUID eventId = UUID.randomUUID();
            when(eventRepo.findById(eventId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.uploadEventImages(eventId, List.of()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Event not found");
        }

        @Test
        @DisplayName("does not resnapshot BEFORE images when event already has snapshot")
        void doesNotResnapshotExistingBeforeImages() {
            AssetEvent e = event();
            AssetEventImage existingBefore = AssetEventImage.builder()
                    .id(UUID.randomUUID())
                    .event(e)
                    .key("asset/current-1.jpg")
                    .type(AssetEventImageType.BEFORE)
                    .createdAt(Instant.now())
                    .build();
            MockMultipartFile newFile = new MockMultipartFile("files", "new.jpg", "image/jpeg", "abc".getBytes());

            when(eventRepo.findById(id)).thenReturn(Optional.of(e));
            when(imageRepo.findByAssetItemId(assetItem.getId())).thenReturn(List.of());
            when(eventImageRepo.findByEventIdAndTypeOrderByCreatedAtAsc(id, AssetEventImageType.BEFORE))
                    .thenReturn(List.of(existingBefore));
            when(eventImageRepo.findByEventIdAndTypeOrderByCreatedAtAsc(id, AssetEventImageType.AFTER))
                    .thenReturn(List.of());
            when(s3.upload(newFile, "asset/" + assetItem.getId())).thenReturn("asset/new.jpg");

            service.uploadEventImages(id, List.of(newFile));

            verify(eventImageRepo, never()).saveAll(any());
            verify(eventImageRepo, never()).deleteAll(any());
            verify(eventImageRepo).save(any(AssetEventImage.class));
        }

        @Test
        @DisplayName("records uploaded file as AFTER even when it matches current image")
        void recordsAfterImageWhenUploadDuplicatesCurrentImage() {
            AssetEvent e = event();
            AssetImage current = AssetImage.builder()
                    .id(UUID.randomUUID())
                    .assetItem(assetItem)
                    .key("asset/current.jpg")
                    .createdAt(Instant.now().minusSeconds(60))
                    .build();
            MockMultipartFile duplicateFile = new MockMultipartFile("files", "same.jpg", "image/jpeg", "abc".getBytes());

            when(eventRepo.findById(id)).thenReturn(Optional.of(e));
            when(imageRepo.findByAssetItemId(assetItem.getId())).thenReturn(List.of(current));
            when(eventImageRepo.findByEventIdAndTypeOrderByCreatedAtAsc(id, AssetEventImageType.BEFORE))
                    .thenReturn(List.of());
            when(eventImageRepo.findByEventIdAndTypeOrderByCreatedAtAsc(id, AssetEventImageType.AFTER))
                    .thenReturn(List.of());
            when(s3.upload(duplicateFile, "asset/" + assetItem.getId())).thenReturn("asset/same-uploaded.jpg");

            service.uploadEventImages(id, List.of(duplicateFile));

            verify(eventImageRepo).saveAll(any());
            verify(imageRepo).deleteAll(List.of(current));
            verify(s3).upload(duplicateFile, "asset/" + assetItem.getId());
            verify(eventImageRepo).save(any(AssetEventImage.class));
        }
    }
}
