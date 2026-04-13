package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.dtos.AssetEventDTO.UpdateAssetEventRequest;
import com.isums.assetservice.domains.entities.AssetEvent;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.enums.AssetEventType;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetEventImageRepository;
import com.isums.assetservice.infrastructures.repositories.AssetEventRepository;
import com.isums.assetservice.infrastructures.repositories.AssetImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
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
        assetItem = AssetItem.builder().id(UUID.randomUUID()).displayName("Cái quạt").build();
    }

    private AssetEvent event() {
        return AssetEvent.builder()
                .id(id).eventType(AssetEventType.CREATED)
                .assetItem(assetItem).createdAt(Instant.now()).build();
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
    @DisplayName("getLatestEvent")
    class Latest {

        @Test
        @DisplayName("maps latest event including images")
        void happy() {
            AssetEvent e = event();
            when(eventRepo.findLatestEvent(assetItem.getId())).thenReturn(List.of(e));
            when(eventImageRepo.findByEventId(id)).thenReturn(List.of());

            AssetEventDto result = service.getLatestEvent(assetItem.getId());
            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getAssetId()).isEqualTo(assetItem.getId());
        }
    }

    @Nested
    @DisplayName("uploadEventImages")
    class Upload {

        @Test
        @DisplayName("throws when event missing")
        void eventMissing() {
            UUID eventId = UUID.randomUUID();
            when(eventRepo.findById(eventId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.uploadEventImages(eventId, List.of()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Event not found");
        }
    }
}
