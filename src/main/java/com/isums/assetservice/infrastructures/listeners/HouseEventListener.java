package com.isums.assetservice.infrastructures.listeners;

import com.isums.assetservice.services.IotThresholdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class HouseEventListener {

    private final IotThresholdService iotThresholdService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "house.created", groupId = "asset-service")
    public void onHouseCreated(String message) {
        try {
            JsonNode node    = objectMapper.readTree(message);
            UUID houseId     = UUID.fromString(node.get("houseId").asString());
            iotThresholdService.seedDefaults(houseId);
            log.info("[KAFKA] seeded thresholds for house {}", houseId);
        } catch (Exception e) {
            log.error("[KAFKA] onHouseCreated failed: {}", e.getMessage());
        }
    }
}
