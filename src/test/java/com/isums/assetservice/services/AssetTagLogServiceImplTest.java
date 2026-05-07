package com.isums.assetservice.services;

import com.isums.assetservice.domains.entities.AssetTagLog;
import com.isums.assetservice.infrastructures.repositories.AssetTagLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssetTagLogServiceImpl")
class AssetTagLogServiceImplTest {

    @Mock private AssetTagLogRepository repo;

    @InjectMocks private AssetTagLogServiceImpl service;

    @Test
    @DisplayName("getLogsByTag returns logs when tagValue valid")
    void byTagHappy() {
        AssetTagLog log = AssetTagLog.builder().tagValue("TAG-1").build();
        when(repo.findByTagValueOrderByCreatedAtDesc("TAG-1")).thenReturn(List.of(log));

        assertThat(service.getLogsByTag("TAG-1")).containsExactly(log);
    }

    @Test
    @DisplayName("getLogsByTag wraps when tagValue null or blank")
    void byTagBlank() {
        assertThatThrownBy(() -> service.getLogsByTag(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error to get logs by tagValue");
        assertThatThrownBy(() -> service.getLogsByTag("  "))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("getLogsByAsset returns logs when assetId provided")
    void byAssetHappy() {
        UUID id = UUID.randomUUID();
        AssetTagLog log = AssetTagLog.builder().newAssetId(id).build();
        when(repo.findAllLogsByAssetId(id)).thenReturn(List.of(log));

        assertThat(service.getLogsByAsset(id)).containsExactly(log);
    }

    @Test
    @DisplayName("getLogsByAsset wraps when assetId null")
    void byAssetNull() {
        assertThatThrownBy(() -> service.getLogsByAsset(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error to get logs by asset");
    }
}
