package com.isums.assetservice.infrastructures.listeners;

import com.isums.assetservice.services.IotThresholdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("HouseEventListener")
class HouseEventListenerTest {

    @Mock private IotThresholdService thresholdService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HouseEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new HouseEventListener(thresholdService, objectMapper);
    }

    @Test
    @DisplayName("seeds default thresholds when house.created message is valid JSON")
    void happy() {
        UUID houseId = UUID.randomUUID();
        listener.onHouseCreated("{\"houseId\":\"" + houseId + "\"}");

        verify(thresholdService).seedDefaults(houseId);
    }

    @Test
    @DisplayName("swallows parse error silently (does not break Kafka stream)")
    void badJson() {
        listener.onHouseCreated("{invalid-json");

        verify(thresholdService, never()).seedDefaults(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("swallows when houseId missing")
    void missingHouseId() {
        listener.onHouseCreated("{}");

        verify(thresholdService, never()).seedDefaults(org.mockito.ArgumentMatchers.any());
    }
}
